/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class SourceEntryErrorReporter extends BuildErrorReporter {

	public SourceEntryErrorReporter(IFile file) {
		super(file);
	}

	class ProjectFolder {
		IPath fPath;
		String fToken;
		ArrayList fLibs = new ArrayList(1);
		String dupeLibName = null;

		public ProjectFolder(IPath path) {
			fPath = path;
		}

		public IPath getPath() {
			return fPath;
		}

		void setToken(String token) {
			fToken = token;
		}

		public String getToken() {
			if (fToken == null) {
				return fPath.toString();
			}
			return fToken;
		}

		public void addLib(String libName) {
			if (fLibs.contains(libName)) {
				dupeLibName = libName;
			} else
				fLibs.add(libName);
		}

		public ArrayList getLibs() {
			return fLibs;
		}

		public String getDupeLibName() {
			return dupeLibName;
		}
	}

	class SourceFolder extends ProjectFolder {

		OutputFolder fOutputFolder;

		/**
		 * Constructs a source folder with the given project relative path.
		 * 
		 * @param path source folder path
		 * @param outputFolder associated output folder
		 */
		public SourceFolder(IPath path, OutputFolder outputFolder) {
			super(path);
			fOutputFolder = outputFolder;
		}

		public OutputFolder getOutputLocation() {
			return fOutputFolder;
		}

	}

	class OutputFolder extends ProjectFolder {

		private ArrayList fSourceFolders = new ArrayList();
		/**
		 * True when there is no corresponding source - i.e. a class file folder or library
		 */
		private boolean fIsLibrary = false;

		/**
		 * Creates an output folder with the given relative path (relative to the project).
		 * 
		 * @param path project relative path
		 */
		public OutputFolder(IPath path) {
			super(path);
		}

		/**
		 * Creates an output folder with the given relative path (relative to the project).
		 * 
		 * @param path project relative path
		 * @param isLibrary whether this output folder is a binary location that has no corresponding
		 *  source folder
		 */
		public OutputFolder(IPath path, boolean isLibrary) {
			this(path);
			fIsLibrary = isLibrary;
		}

		public void addSourceFolder(SourceFolder sourceFolder) {
			if (!fSourceFolders.contains(sourceFolder))
				fSourceFolders.add(sourceFolder);
		}

		public boolean isLibrary() {
			return fIsLibrary;
		}

		public ArrayList getSourceFolders() {
			return fSourceFolders;
		}

	}

	private HashMap fSourceFolderMap = new HashMap(4);
	private HashMap fOutputFolderMap = new HashMap(4);

	public void initialize(ArrayList sourceEntries, ArrayList outputEntries, IClasspathEntry[] cpes, IProject project) {

		fProject = project;
		IPath defaultOutputLocation = null;
		IJavaProject javaProject = JavaCore.create(fProject);
		try {
			defaultOutputLocation = javaProject.getOutputLocation();
		} catch (JavaModelException e) {
		}

		for (int i = 0; i < cpes.length; i++) {
			if (cpes[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath sourcePath = cpes[i].getPath().removeFirstSegments(1).addTrailingSeparator();
				IPath outputLocation = cpes[i].getOutputLocation();
				if (outputLocation == null)
					outputLocation = defaultOutputLocation;
				IPath outputPath = outputLocation.removeFirstSegments(1).addTrailingSeparator();

				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputPath);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(outputPath);
				}

				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourcePath);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(sourcePath, outputFolder);
				}

				outputFolder.addSourceFolder(sourceFolder);
				fOutputFolderMap.put(outputPath, outputFolder);
				fSourceFolderMap.put(sourcePath, sourceFolder);
			} else if (cpes[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IClasspathEntry entry = cpes[i];
				IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(entry);
				IPath outputPath = null;
				if (roots.length == 1) { // should only be one entry for a library
					if (roots[0].getResource() != null) { // in the workspace
						outputPath = entry.getPath().removeFirstSegments(1).addTrailingSeparator();
					} else { // external
						outputPath = entry.getPath();
					}
				}
				OutputFolder outputFolder = new OutputFolder(outputPath, true);
				fOutputFolderMap.put(outputPath, outputFolder);
			}
		}

		for (Iterator iterator = sourceEntries.iterator(); iterator.hasNext();) {
			IBuildEntry sourceEntry = (IBuildEntry) iterator.next();
			String libName = sourceEntry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
			String[] tokens = sourceEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				IPath path = new Path(tokens[i]).addTrailingSeparator();
				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(path);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(path, null);
					fSourceFolderMap.put(path, sourceFolder);
				}
				sourceFolder.setToken(tokens[i]);
				sourceFolder.addLib(libName);
			}
		}

		for (Iterator iterator = outputEntries.iterator(); iterator.hasNext();) {
			IBuildEntry outputEntry = (IBuildEntry) iterator.next();
			String libName = outputEntry.getName().substring(PROPERTY_OUTPUT_PREFIX.length());
			String[] tokens = outputEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				IPath path = new Path(tokens[i]).addTrailingSeparator();
				if (path.segmentCount() == 1 && path.segment(0).equals(".")) { //$NON-NLS-1$
					// translate "." to root path
					path = Path.ROOT;
				}
				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(path);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(path);
					fOutputFolderMap.put(path, outputFolder);
				}
				outputFolder.setToken(tokens[i]);
				outputFolder.addLib(libName);
			}
		}
	}

	public void validate() {

		for (Iterator iterator = fOutputFolderMap.keySet().iterator(); iterator.hasNext();) {
			IPath outputPath = (IPath) iterator.next();
			OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputPath);
			ArrayList sourceFolders = outputFolder.getSourceFolders();
			ArrayList outputFolderLibs = new ArrayList(outputFolder.getLibs());

			if (sourceFolders.size() == 0) {
				if (!outputFolder.isLibrary()) {
					// report error - invalid output folder				
					for (Iterator libNameiterator = outputFolderLibs.iterator(); libNameiterator.hasNext();) {
						String libName = (String) libNameiterator.next();
						IResource folderEntry = fProject.findMember(outputPath);
						String message;
						if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer))
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, outputPath.toString());
						else
							message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidOutputFolder, outputPath.toString());
						prepareError(PROPERTY_OUTPUT_PREFIX + libName, outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fOututLibSeverity, PDEMarkerFactory.CAT_OTHER);
					}
				}
			} else {
				String srcFolderLibName = null;

				for (int i = 0; i < sourceFolders.size(); i++) {
					SourceFolder sourceFolder = (SourceFolder) sourceFolders.get(i);
					ArrayList srcFolderLibs = sourceFolder.getLibs();
					outputFolderLibs.removeAll(srcFolderLibs);
					switch (srcFolderLibs.size()) {
						case 0 :
							//error - src folder with no lib
							//do nothing. already caught in super
							break;
						case 1 :
							if (srcFolderLibName == null) {
								srcFolderLibName = (String) srcFolderLibs.get(0);
								break;
							} else if (srcFolderLibName.equals(srcFolderLibs.get(0))) {
								break;
							}
						default :
							//error - targeted to diff libs
							String erringSrcFolders = join((SourceFolder[]) sourceFolders.toArray(new SourceFolder[sourceFolders.size()]));
							for (int j = 0; j < sourceFolders.size(); j++) {
								SourceFolder srcFolder = (SourceFolder) sourceFolders.get(j);
								for (int k = 0; k < srcFolder.getLibs().size(); k++) {
									String libName = (String) srcFolder.getLibs().get(k);
									String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DifferentTargetLibrary, erringSrcFolders);
									prepareError(PROPERTY_SOURCE_PREFIX + libName, srcFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
								}
							}
					}
				}
				for (int i = 0; i < outputFolderLibs.size(); i++) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_ExtraOutputFolder, outputFolder.getPath().toString(), PROPERTY_SOURCE_PREFIX + outputFolderLibs.get(i));
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolderLibs.get(i), outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fOututLibSeverity, PDEMarkerFactory.CAT_OTHER);
				}

				if (outputFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeOutputFolder, outputPath.toString(), PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName());
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName(), outputFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, fOututLibSeverity, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

		HashMap missingOutputEntryErrors = new HashMap(4);
		class MissingOutputEntry {
			public StringBuffer fSsrcFolders = new StringBuffer();
			public StringBuffer fOutputFolders = new StringBuffer();

			public String get(StringBuffer field) {
				if (field.charAt(field.length() - 1) == ',') {
					field.deleteCharAt(field.length() - 1);
				}
				return field.toString().trim();
			}
		}

		for (Iterator iterator = fSourceFolderMap.keySet().iterator(); iterator.hasNext();) {
			IPath sourcePath = (IPath) iterator.next();
			SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourcePath);
			OutputFolder outputFolder = sourceFolder.getOutputLocation();

			if (outputFolder == null) {
				//error - not a src folder
				IResource folderEntry = fProject.findMember(sourcePath);
				String message;
				if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer))
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, sourcePath.toString());
				else
					message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidSourceFolder, sourcePath.toString());

				ArrayList srcLibs = sourceFolder.getLibs();
				for (int i = 0; i < srcLibs.size(); i++) {
					String libName = (String) srcLibs.get(i);
					prepareError(PROPERTY_SOURCE_PREFIX + libName, sourceFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
				}
			} else {
				if (outputFolder.getLibs().size() == 0 && sourceFolder.getLibs().size() == 1) {
					//error - missing output folder

					String libName = (String) sourceFolder.getLibs().get(0);
					MissingOutputEntry errorEntry = (MissingOutputEntry) missingOutputEntryErrors.get(libName);
					if (errorEntry == null)
						errorEntry = new MissingOutputEntry();

					if (errorEntry.fSsrcFolders.indexOf(sourcePath.toString() + ',') < 0) {
						errorEntry.fSsrcFolders.append(' ' + sourcePath.toString() + ',');
					}

					if (errorEntry.fOutputFolders.indexOf(outputFolder.getToken() + ',') < 0) {
						errorEntry.fOutputFolders.append(' ' + outputFolder.getToken() + ',');
					}
					missingOutputEntryErrors.put(libName, errorEntry);
				}

				if (sourceFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeSourceFolder, sourcePath.toString(), PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName());
					prepareError(PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName(), sourceFolder.getToken(), message, PDEMarkerFactory.NO_RESOLUTION, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

		for (Iterator iter = missingOutputEntryErrors.keySet().iterator(); iter.hasNext();) {
			String libName = (String) iter.next();
			MissingOutputEntry errorEntry = (MissingOutputEntry) missingOutputEntryErrors.get(libName);
			String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingOutputEntry, errorEntry.get(errorEntry.fSsrcFolders), PROPERTY_OUTPUT_PREFIX + libName);
			prepareError(PROPERTY_OUTPUT_PREFIX + libName, errorEntry.get(errorEntry.fOutputFolders), message, PDEMarkerFactory.B_ADDDITION, fMissingOutputLibSeverity, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private String join(ProjectFolder[] folders) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < folders.length; i++) {
			String text = folders[i].getPath().toString().trim();
			if (text.length() > 0) {
				result.append(text);
				result.append(',');
			}
		}
		result.deleteCharAt(result.length() - 1);
		return result.toString();
	}

	public ArrayList getProblemList() {
		return fProblemList;
	}
}
