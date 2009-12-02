/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.PDECoreMessages;

public class SourceEntryErrorReporter extends BuildErrorReporter {

	public SourceEntryErrorReporter(IFile file, int buildSeverity) {
		super(file);
		fBuildSeverity = buildSeverity;
	}

	class ProjectFolder {
		String fFolderName;
		ArrayList fLibs = new ArrayList(1);
		String dupeLibName = null;

		public ProjectFolder(String folder) {
			fFolderName = folder;
		}

		public String getName() {
			return fFolderName;
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

		public SourceFolder(String folder, OutputFolder outputFolder) {
			super(folder);
			fOutputFolder = outputFolder;
		}

		public OutputFolder getOutputLocation() {
			return fOutputFolder;
		}

	}

	class OutputFolder extends ProjectFolder {

		private ArrayList fSourceFolders = new ArrayList();

		public OutputFolder(String folder) {
			super(folder);
		}

		public void addSourceFolder(SourceFolder sourceFolder) {
			if (!fSourceFolders.contains(sourceFolder))
				fSourceFolders.add(sourceFolder);
		}

		public ArrayList getSourceFolders() {
			return fSourceFolders;
		}

	}

	HashMap fSourceFolderMap = new HashMap(4);
	HashMap fOutputFolderMap = new HashMap(4);

	public void initialize(ArrayList sourceEntries, ArrayList outputEntries, IClasspathEntry[] cpes, IProject project) {

		fProject = project;
		IPath defaultOutputLocation = null;
		try {
			defaultOutputLocation = JavaCore.create(fProject).getOutputLocation();
		} catch (JavaModelException e) {
		}

		for (int i = 0; i < cpes.length; i++) {
			if (cpes[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				String sourceFolderName = cpes[i].getPath().removeFirstSegments(1).addTrailingSeparator().toString();
				IPath outputLocation = cpes[i].getOutputLocation();
				if (outputLocation == null)
					outputLocation = defaultOutputLocation;
				String outputFolderName = outputLocation.removeFirstSegments(1).addTrailingSeparator().toString();

				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputFolderName);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(outputFolderName);
				}

				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourceFolderName);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(sourceFolderName, outputFolder);
				}

				outputFolder.addSourceFolder(sourceFolder);
				fOutputFolderMap.put(outputFolderName, outputFolder);
				fSourceFolderMap.put(sourceFolderName, sourceFolder);
			}
		}

		for (Iterator iterator = sourceEntries.iterator(); iterator.hasNext();) {
			IBuildEntry sourceEntry = (IBuildEntry) iterator.next();
			String libName = sourceEntry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
			String[] tokens = sourceEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(tokens[i]);
				if (sourceFolder == null) {
					sourceFolder = new SourceFolder(tokens[i], null);
					fSourceFolderMap.put(tokens[i], sourceFolder);
				}
				sourceFolder.addLib(libName);
			}
		}

		for (Iterator iterator = outputEntries.iterator(); iterator.hasNext();) {
			IBuildEntry outputEntry = (IBuildEntry) iterator.next();
			String libName = outputEntry.getName().substring(PROPERTY_OUTPUT_PREFIX.length());
			String[] tokens = outputEntry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(tokens[i]);
				if (outputFolder == null) {
					outputFolder = new OutputFolder(tokens[i]);
					fOutputFolderMap.put(tokens[i], outputFolder);
				}
				outputFolder.addLib(libName);
			}
		}
	}

	public void validate() {

		for (Iterator iterator = fOutputFolderMap.keySet().iterator(); iterator.hasNext();) {
			String outputFolderName = (String) iterator.next();
			OutputFolder outputFolder = (OutputFolder) fOutputFolderMap.get(outputFolderName);
			ArrayList sourceFolders = outputFolder.getSourceFolders();
			ArrayList outputFolderLibs = new ArrayList(outputFolder.getLibs());

			if (sourceFolders.size() == 0) {
				// report error - invalid output folder				
				for (Iterator libNameiterator = outputFolderLibs.iterator(); libNameiterator.hasNext();) {
					String libName = (String) libNameiterator.next();
					IResource folderEntry = fProject.findMember(outputFolderName);
					String message;
					if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IFolder))
						message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, outputFolderName);
					else
						message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidOutputFolder, outputFolderName);
					prepareError(PROPERTY_OUTPUT_PREFIX + libName, outputFolderName, message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
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
								String srcFolderName = srcFolder.getName();
								for (int k = 0; k < srcFolder.getLibs().size(); k++) {
									String libName = (String) srcFolder.getLibs().get(k);
									String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DifferentTargetLibrary, erringSrcFolders);
									prepareError(PROPERTY_SOURCE_PREFIX + libName, srcFolderName, message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
								}
							}
					}
				}
				for (int i = 0; i < outputFolderLibs.size(); i++) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_ExtraOutputFolder, outputFolder.getName(), PROPERTY_SOURCE_PREFIX + outputFolderLibs.get(i));
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolderLibs.get(i), outputFolder.getName(), message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
				}

				if (outputFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeOutputFolder, outputFolderName, PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName());
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName(), outputFolderName, message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

		for (Iterator iterator = fSourceFolderMap.keySet().iterator(); iterator.hasNext();) {
			String sourceFolderName = (String) iterator.next();
			SourceFolder sourceFolder = (SourceFolder) fSourceFolderMap.get(sourceFolderName);
			OutputFolder outputFolder = sourceFolder.getOutputLocation();

			if (outputFolder == null) {
				//error - not a src folder
				IResource folderEntry = fProject.findMember(sourceFolderName);
				String message;
				if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IFolder))
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, sourceFolderName);
				else
					message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidSourceFolder, sourceFolderName);

				ArrayList srcLibs = sourceFolder.getLibs();
				for (int i = 0; i < srcLibs.size(); i++) {
					String libName = (String) srcLibs.get(i);
					prepareError(PROPERTY_SOURCE_PREFIX + libName, sourceFolderName, message, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_OTHER);
				}
			} else {
				if (sourceFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeSourceFolder, sourceFolderName, PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName());
					prepareError(PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName(), sourceFolderName, message, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}

	}

	private String join(ProjectFolder[] folders) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < folders.length; i++) {
			if (folders[i].getName().trim().length() > 0) {
				result.append(folders[i].getName().trim());
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
