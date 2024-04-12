/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.project.PDEProject;

public class SourceEntryErrorReporter extends BuildErrorReporter {

	private static final String DEF_OUTPUT_ENTRY = PROPERTY_OUTPUT_PREFIX + '.';

	public SourceEntryErrorReporter(IFile file, IBuild model) {
		super(file);
		fBuild = model;
	}

	private static class ProjectFolder {
		private final IPath fPath;
		private String fToken;
		private final List<String> fLibs = new ArrayList<>(1);
		private String dupeLibName;

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
			} else {
				fLibs.add(libName);
			}
		}

		public List<String> getLibs() {
			return fLibs;
		}

		public String getDupeLibName() {
			return dupeLibName;
		}
	}

	private static class SourceFolder extends ProjectFolder {

		private final OutputFolder fOutputFolder;

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

	private static class OutputFolder extends ProjectFolder {

		private final List<SourceFolder> fSourceFolders = new ArrayList<>();
		/**
		 * True when there is no corresponding source - i.e. a class file folder or library
		 */
		private final boolean fIsLibrary;

		/**
		 * Creates an output folder with the given relative path (relative to the project).
		 *
		 * @param path project relative path
		 */
		public OutputFolder(IPath path) {
			this(path, false);
		}

		/**
		 * Creates an output folder with the given relative path (relative to the project).
		 *
		 * @param path project relative path
		 * @param isLibrary whether this output folder is a binary location that has no corresponding
		 *  source folder
		 */
		public OutputFolder(IPath path, boolean isLibrary) {
			super(path);
			fIsLibrary = isLibrary;
		}

		public void addSourceFolder(SourceFolder sourceFolder) {
			if (!fSourceFolders.contains(sourceFolder)) {
				fSourceFolders.add(sourceFolder);
			}
		}

		public boolean isLibrary() {
			return fIsLibrary;
		}

		public List<SourceFolder> getSourceFolders() {
			return fSourceFolders;
		}

	}

	/**
	 * Represents a default or custom encoding property for a resource
	 * within a library.
	 */
	static class EncodingEntry {

		private final String fEncoding;
		private final IResource fResource;

		/**
		 * Constructs an encoding entry for the given resource.
		 *
		 * @param resource resource
		 * @param encoding the encoding identifier
		 */
		EncodingEntry(IResource resource, String encoding) {
			fEncoding = encoding;
			fResource = resource;
		}

		/**
		 * Returns the explicit encoding for this entry.
		 *
		 * @return explicit encoding
		 */
		public String getEncoding() {
			return fEncoding;
		}

		/**
		 * Returns the resource this encoding is associated with.
		 *
		 * @return associated resource
		 */
		public IResource getResource() {
			return fResource;
		}

		@Override
		public String toString() {
			return getValue();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EncodingEntry other) {
				return other.fEncoding.equals(fEncoding) && other.fResource.equals(fResource);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return fEncoding.hashCode() + fResource.hashCode();
		}

		/**
		 * Returns the generated value of this entry for the build.properties file.
		 *
		 * @return value to enter into build.properties
		 */
		String getValue() {
			StringBuilder buf = new StringBuilder();
			IContainer root = PDEProject.getBundleRoot(fResource.getProject());
			buf.append(fResource.getFullPath().makeRelativeTo(root.getFullPath()).makeAbsolute());
			buf.append('[');
			buf.append(fEncoding);
			buf.append(']');
			return buf.toString();
		}

	}

	/**
	 * Visits a source folder gathering encodings.
	 */
	class Visitor implements IResourceVisitor {

		String[] fLibs = null;

		Visitor(SourceFolder folder) {
			List<String> list = folder.getLibs();
			fLibs = list.toArray(new String[list.size()]);
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			String encoding = null;
			switch (resource.getType()) {
				case IResource.FOLDER :
					encoding = ((IFolder) resource).getDefaultCharset(false);
					break;
				case IResource.FILE :
					IFile file = (IFile) resource;
					// only worry about .java files
					if (file.getFileExtension() != null && file.getFileExtension().equals("java")) { //$NON-NLS-1$
						encoding = file.getCharset(false);
					}
					break;
			}
			if (encoding != null) {
				EncodingEntry entry = new EncodingEntry(resource, encoding);
				for (String lib : fLibs) {
					List<EncodingEntry> encodings = fCustomEncodings.get(lib);
					if (encodings == null) {
						encodings = new ArrayList<>();
						fCustomEncodings.put(lib, encodings);
					}
					encodings.add(entry);
				}
			}
			return true;
		}

	}

	private final Map<IPath, SourceFolder> fSourceFolderMap = new HashMap<>(4);
	private final Map<IPath, OutputFolder> fOutputFolderMap = new HashMap<>(4);
	private IBuild fBuild = null;

	/**
	 * Maps library name to custom {@link EncodingEntry}'s for this library.
	 */
	Map<String, List<EncodingEntry>> fCustomEncodings = new HashMap<>();

	public void initialize(List<IBuildEntry> sourceEntries, List<IBuildEntry> outputEntries, IClasspathEntry[] cpes) {
		IPath defaultOutputLocation = null;
		IJavaProject javaProject = JavaCore.create(fProject);
		try {
			defaultOutputLocation = javaProject.getOutputLocation();
		} catch (JavaModelException e) {
		}

		Set<String> pluginLibraryNames = new HashSet<>(2);
		IPluginModelBase pluginModel = PluginRegistry.findModel(fProject);
		if (pluginModel != null) {
			IPluginLibrary[] pluginLibraries = pluginModel.getPluginBase().getLibraries();
			for (IPluginLibrary library : pluginLibraries) {
				pluginLibraryNames.add(library.getName());
			}
			for (IPluginExtension extension : pluginModel.getPluginBase().getExtensions()) {
				if ("org.eclipse.ant.core.extraClasspathEntries".equals(extension.getPoint())) { //$NON-NLS-1$
					for (IPluginObject pluginObject : extension.getChildren()) {
						if ("extraClasspathEntry".equals(pluginObject.getName()) //$NON-NLS-1$
								&& pluginObject instanceof IPluginElement element) {
							IPluginAttribute library = element.getAttribute("library"); //$NON-NLS-1$
							if (library != null) {
								pluginLibraryNames.add(library.getValue().trim());
							}
						}
					}
				}
			}
		}
		pluginLibraryNames.add("."); //$NON-NLS-1$ )
		for (IClasspathEntry entry : cpes) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath sourcePath = getPath(entry);
				if (sourcePath == null) {
					continue;
				}

				IPath outputLocation = entry.getOutputLocation();
				if (outputLocation == null) {
					outputLocation = defaultOutputLocation;
				}
				IPath outputPath = getPath(outputLocation);

				OutputFolder outputFolder = fOutputFolderMap.computeIfAbsent(outputPath, OutputFolder::new);
				SourceFolder sourceFolder = fSourceFolderMap.computeIfAbsent(sourcePath,
						p -> new SourceFolder(p, outputFolder));

				outputFolder.addSourceFolder(sourceFolder);
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(entry);
				if (roots.length == 1 && !roots[0].isArchive()) {
					// should only be one entry for a library
					IPath outputPath = getPath(entry);
					OutputFolder outputFolder = new OutputFolder(outputPath, true);
					fOutputFolderMap.put(outputPath, outputFolder);
				}
			}
		}

		for (IBuildEntry sourceEntry : sourceEntries) {
			String libName = sourceEntry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
			if (!pluginLibraryNames.contains(libName)) {
				prepareError(sourceEntry.getName(), null, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingLibrary, libName), PDEMarkerFactory.B_REMOVAL, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
			}
			String[] tokens = sourceEntry.getTokens();
			for (final String token : tokens) {
				IPath path = IPath.fromOSString(token).addTrailingSeparator();
				SourceFolder sourceFolder = fSourceFolderMap.computeIfAbsent(path, p -> new SourceFolder(p, null));
				sourceFolder.setToken(token);
				sourceFolder.addLib(libName);
			}
		}

		for (IBuildEntry outputEntry : outputEntries) {
			String libName = outputEntry.getName().substring(PROPERTY_OUTPUT_PREFIX.length());
			if (!pluginLibraryNames.contains(libName)) {
				prepareError(outputEntry.getName(), null, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingLibrary, libName), PDEMarkerFactory.B_REMOVAL, fOututLibSeverity,CompilerFlags.P_BUILD_OUTPUT_LIBRARY, PDEMarkerFactory.CAT_OTHER);
			}
			String[] tokens = outputEntry.getTokens();
			for (String token : tokens) {
				IPath path = IPath.fromOSString(token).addTrailingSeparator();
				if (path.segmentCount() == 1 && path.segment(0).equals(".")) { //$NON-NLS-1$
					// translate "." to root path
					path = IPath.ROOT;
				}
				OutputFolder outputFolder = fOutputFolderMap.computeIfAbsent(path, OutputFolder::new);
				outputFolder.setToken(token);
				outputFolder.addLib(libName);
			}
		}
	}

	private IPath getPath(Object entry) {
		IPath path = null;
		if (entry instanceof IClasspathEntry cpes) {
			path = cpes.getPath();
		} else if (entry instanceof IPath entryPath) {
			path = entryPath;
		}
		if (path != null && path.matchingFirstSegments(fProject.getFullPath()) > 0) {
			path = path.removeFirstSegments(1);
		}
		if (path != null) {
			return path.addTrailingSeparator();
		}
		return null;
	}

	public void validate() {
		validateOutputEntries();
		List<SourceFolder> toValidate = validateSourceEntries();
		validateSourceEncodings(toValidate);
	}

	private void validateOutputEntries() {
		fOutputFolderMap.forEach((outputPath, outputFolder) -> {
			List<SourceFolder> sourceFolders = outputFolder.getSourceFolders();
			List<String> outputFolderLibs = new ArrayList<>(outputFolder.getLibs());

			if (sourceFolders.isEmpty()) {
				if (!outputFolder.isLibrary()) {
					// report error - invalid output folder
					for (String libName : outputFolderLibs) {
						IResource folderEntry = fProject.findMember(outputPath);
						String message;
						if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer)) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, outputPath.toString());
						} else {
							message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidOutputFolder, outputPath.toString());
						}
						prepareError(PROPERTY_OUTPUT_PREFIX + libName, outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fOututLibSeverity, CompilerFlags.P_BUILD_OUTPUT_LIBRARY,PDEMarkerFactory.CAT_OTHER);
					}
				} else {
					if (outputFolderLibs.isEmpty()) {
						//class folder does not have an output.<library> entry, only continue if we have a plugin model for the project
						IPluginModelBase model = PluginRegistry.findModel(fProject);
						if (model != null) {
							IPluginLibrary[] libs = model.getPluginBase().getLibraries();
							String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingOutputLibForClassFolder, outputPath.toString());
							if (libs.length > 0) {
								prepareError(PROPERTY_OUTPUT_PREFIX, null, message, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fOututLibSeverity,CompilerFlags.P_BUILD_OUTPUT_LIBRARY, PDEMarkerFactory.CAT_OTHER);
							} else {
								prepareError(DEF_OUTPUT_ENTRY, outputPath.toString(), message, PDEMarkerFactory.B_ADDITION, fOututLibSeverity, CompilerFlags.P_BUILD_OUTPUT_LIBRARY,PDEMarkerFactory.CAT_OTHER);
							}
						}

					}
				}
			} else {
				String srcFolderLibName = null;

				for (SourceFolder sourceFolder : sourceFolders) {
					List<String> srcFolderLibs = sourceFolder.getLibs();
					outputFolderLibs.removeAll(srcFolderLibs);
					switch (srcFolderLibs.size()) {
						case 0 :
							//error - src folder with no lib
							//do nothing. already caught in super
							break;
						case 1 :
							if (srcFolderLibName == null) {
								srcFolderLibName = srcFolderLibs.get(0);
								break;
							} else if (srcFolderLibName.equals(srcFolderLibs.get(0))) {
								break;
							}
						default :
							//error - targeted to diff libs
							String erringSrcFolders = sourceFolders.stream().map(f -> f.getPath().toString().trim())
									.filter(s -> !s.isEmpty()).collect(Collectors.joining(",")); //$NON-NLS-1$
							for (SourceFolder srcFolder : sourceFolders) {
								for (String libName : srcFolder.getLibs()) {
									String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DifferentTargetLibrary, erringSrcFolders);
									prepareError(PROPERTY_SOURCE_PREFIX + libName, srcFolder.getToken(), message, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
								}
							}
					}
				}
				for (String outputFolderLib : outputFolderLibs) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_ExtraOutputFolder, outputFolder.getPath().toString(), PROPERTY_SOURCE_PREFIX + outputFolderLib);
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolderLib, outputFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fOututLibSeverity,CompilerFlags.P_BUILD_OUTPUT_LIBRARY, PDEMarkerFactory.CAT_OTHER);
				}

				if (outputFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeOutputFolder, outputPath.toString(), PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName());
					prepareError(PROPERTY_OUTPUT_PREFIX + outputFolder.getDupeLibName(), outputFolder.getToken(), message, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fOututLibSeverity, CompilerFlags.P_BUILD_OUTPUT_LIBRARY,PDEMarkerFactory.CAT_OTHER);
				}
			}
		});
	}

	private List<SourceFolder> validateSourceEntries() {

		class MissingOutputEntry {
			private final List<String> fSrcFolders = new ArrayList<>(1);
			private final List<String> fOutputFolders = new ArrayList<>(1);

			public String getOutputList() {
				return generateList(fOutputFolders);
			}

			public String getSourceList() {
				return generateList(fSrcFolders);
			}

			private String generateList(List<String> strings) {
				StringBuilder buffer = new StringBuilder();
				Iterator<String> iterator = strings.iterator();
				while (iterator.hasNext()) {
					String next = iterator.next();
					buffer.append(next);
					if (iterator.hasNext()) {
						buffer.append(',');
						buffer.append(' ');
					}
				}
				return buffer.toString();
			}

			public void addSrcFolder(String sourcePath) {
				if (!fSrcFolders.contains(sourcePath)) {
					fSrcFolders.add(sourcePath);
				}
			}

			public void addOutputFolder(String outputPath) {
				if (!fOutputFolders.contains(outputPath)) {
					fOutputFolders.add(outputPath);
				}
			}
		}

		Map<String, MissingOutputEntry> missingOutputEntryErrors = new HashMap<>(4);

		List<SourceFolder> toValidate = new ArrayList<>(); // list of source folders to perform encoding validation on
		fSourceFolderMap.forEach((sourcePath, sourceFolder) -> {
			OutputFolder outputFolder = sourceFolder.getOutputLocation();

			if (outputFolder == null) {
				//error - not a src folder
				IResource folderEntry = fProject.findMember(sourcePath);
				String message;
				if (folderEntry == null || !folderEntry.exists() || !(folderEntry instanceof IContainer)) {
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, sourcePath.toString());
				} else {
					message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_InvalidSourceFolder, sourcePath.toString());
				}

				for (String libName : sourceFolder.getLibs()) {
					prepareError(PROPERTY_SOURCE_PREFIX + libName, sourceFolder.getToken(), message, PDEMarkerFactory.B_REMOVAL, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
				}
			} else {
				if (outputFolder.getLibs().isEmpty() && sourceFolder.getLibs().size() == 1) {
					//error - missing output folder

					String libName = sourceFolder.getLibs().get(0);
					MissingOutputEntry errorEntry = missingOutputEntryErrors.computeIfAbsent(libName,
							n -> new MissingOutputEntry());
					errorEntry.addSrcFolder(sourcePath.toString());
					errorEntry.addOutputFolder(outputFolder.getToken());
				}

				if (sourceFolder.getDupeLibName() != null) {
					String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_DupeSourceFolder, sourcePath.toString(), PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName());
					prepareError(PROPERTY_SOURCE_PREFIX + sourceFolder.getDupeLibName(), sourceFolder.getToken(), message, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
				}

				toValidate.add(sourceFolder);
			}
		});

		missingOutputEntryErrors.forEach((libName, errorEntry) -> {
			String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_MissingOutputEntry, errorEntry.getSourceList(), PROPERTY_OUTPUT_PREFIX + libName);
			prepareError(PROPERTY_OUTPUT_PREFIX + libName, errorEntry.getOutputList(), message, PDEMarkerFactory.B_ADDITION, fMissingOutputLibSeverity,CompilerFlags.P_BUILD_MISSING_OUTPUT, PDEMarkerFactory.CAT_OTHER);
		});
		return toValidate;
	}

	private void validateSourceEncodings(List<SourceFolder> toValidate) {
		// validate workspace encodings with those specified in build.properties

		if (fEncodingSeverity == CompilerFlags.ERROR || fEncodingSeverity == CompilerFlags.WARNING
				|| fEncodingSeverity == CompilerFlags.INFO) {
			// Maps library name to default encoding for that library (or not
			// present if there is no explicit default encoding specified).
			Map<String, String> defaultLibraryEncodings = new HashMap<>();
			// build map of expected encodings
			for (SourceFolder sourceFolder : toValidate) {
				IPath sourcePath = sourceFolder.getPath();
				IContainer container = fProject;
				if (!sourcePath.isEmpty() && !sourcePath.isRoot()) {
					container = container.getFolder(sourcePath);
				}
				try {
					String encoding = getExplicitEncoding(container);
					if (encoding != null) {
						for (String lib : sourceFolder.getLibs()) {
							defaultLibraryEncodings.put(lib, encoding);
						}
					}
					container.accept(new Visitor(sourceFolder));
				} catch (CoreException e) {
					// Can't validate if unable to retrieve encoding
					PDECore.log(e);
				}

			}

			// Compare to encodings specified in build.properties (if any)
			IBuildEntry[] entries = fBuild.getBuildEntries();
			for (IBuildEntry entry : entries) {
				String name = entry.getName();
				if (name.startsWith(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX)) {
					String lib = name.substring(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX.length());
					String[] tokens = entry.getTokens();
					if (tokens.length > 0) {
						if (tokens.length == 1) {
							// compare
							String specified = tokens[0];
							String expected = defaultLibraryEncodings.remove(lib);
							if (expected != null) {
								if (!specified.equals(expected)) {
									prepareError(name, expected, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_0, new String[] {expected, specified, lib}), PDEMarkerFactory.B_REPLACE, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
								}
							} else {
								// encoding is specified, but workspace does not specify one
								prepareError(name, null, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_1, new String[] {specified, lib}), PDEMarkerFactory.B_REMOVAL, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS,PDEMarkerFactory.CAT_OTHER);
							}
						} else {
							// syntax error
							defaultLibraryEncodings.remove(lib);
							prepareError(name, null, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_2, lib), PDEMarkerFactory.M_ONLY_CONFIG_SEV, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
						}
					}
				} else if (name.startsWith(PROPERTY_JAVAC_CUSTOM_ENCODINGS_PREFIX)) {
					IContainer bundleRoot = PDEProject.getBundleRoot(fProject);
					String lib = name.substring(PROPERTY_JAVAC_CUSTOM_ENCODINGS_PREFIX.length());
					String[] tokens = entry.getTokens();
					if (tokens.length > 0) {
						List<EncodingEntry> encodings = new ArrayList<>();
						for (String special : tokens) {
							int index = special.indexOf('[');
							if (index >= 0 && special.endsWith("]")) { //$NON-NLS-1$
								String path = special.substring(0, index);
								String encoding = special.substring(index + 1, special.length() - 1);
								IResource member = bundleRoot.findMember(path);
								if (member == null) {
									// error - missing resource
									String message = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_3, new String[] {encoding, path});
									prepareError(name, special, message, PDEMarkerFactory.B_REMOVAL, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
								} else {
									encodings.add(new EncodingEntry(member, encoding));
								}
							} else {
								// syntax error - invalid
								String message = PDECoreMessages.SourceEntryErrorReporter_4;
								prepareError(name, special, message, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
							}
						}
						// compare with workspace encodings
						List<EncodingEntry> workspace = fCustomEncodings.remove(lib);
						if (workspace == null) {
							prepareError(name, null, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_5, lib), PDEMarkerFactory.B_REMOVAL,fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
						} else {
							Map<IResource, String> map = new HashMap<>();
							for (EncodingEntry ee : workspace) {
								map.put(ee.getResource(), ee.getEncoding());
							}
							for (EncodingEntry ee : encodings) {
								String specified = ee.getEncoding();
								String expected = map.remove(ee.getResource());
								if (expected == null) {
									prepareError(name, ee.getValue(), NLS.bind(PDECoreMessages.SourceEntryErrorReporter_6, new String[] {expected, ee.getResource().getProjectRelativePath().toString()}), PDEMarkerFactory.B_REMOVAL, fEncodingSeverity, CompilerFlags.P_BUILD_ENCODINGS,PDEMarkerFactory.CAT_OTHER);
								} else {
									if (!specified.equals(expected)) {
										prepareError(name, ee.getValue(), NLS.bind(PDECoreMessages.SourceEntryErrorReporter_7, new String[] {expected, ee.getResource().getProjectRelativePath().toString(), specified}), PDEMarkerFactory.M_ONLY_CONFIG_SEV, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
									}
								}
							}
							// anything left in the workspace map?
							if (!map.isEmpty()) {
								map.forEach((res, expected) -> {
									String missing = new EncodingEntry(res, expected).toString();
									String m = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_8, new String[] {expected, res.getProjectRelativePath().toString()});
									prepareError(name, missing, m, PDEMarkerFactory.B_ADDITION, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
								});
							}
						}
					}

				}
			}

			// check for unspecified default encodings
			defaultLibraryEncodings.forEach((lib, expected) -> {
				prepareError(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX + lib, expected, NLS.bind(PDECoreMessages.SourceEntryErrorReporter_9, new String[] {expected, lib}), PDEMarkerFactory.B_ADDITION, fEncodingSeverity,CompilerFlags.P_BUILD_ENCODINGS, PDEMarkerFactory.CAT_OTHER);
			});

			// check for unspecified custom encodings
			fCustomEncodings.forEach((lib, encodings) -> {
				for (EncodingEntry encoding : encodings) {
					String m = NLS.bind(PDECoreMessages.SourceEntryErrorReporter_8, new String[] {encoding.getEncoding(), encoding.getResource().getProjectRelativePath().toString()});
					prepareError(PROPERTY_JAVAC_CUSTOM_ENCODINGS_PREFIX + lib, encoding.toString(), m, PDEMarkerFactory.B_ADDITION, fEncodingSeverity, CompilerFlags.P_BUILD_ENCODINGS,PDEMarkerFactory.CAT_OTHER);
				}
			});
		}
	}

	/**
	 * Returns any explicit encoding set on the given container or one of its parents
	 * up to and including its project.
	 *
	 * @param container container
	 * @return any explicit encoding or <code>null</code> if none
	 */
	private String getExplicitEncoding(IContainer container) throws CoreException {
		String encoding = container.getDefaultCharset(false);
		if (encoding == null) {
			IContainer parent = container.getParent();
			if (parent != null) {
				switch (parent.getType()) {
					case IResource.FOLDER :
						return getExplicitEncoding(parent);
					case IResource.PROJECT :
						return getExplicitEncoding(parent);
					default :
						// don't consider workspace encoding
						return null;
				}
			}
		}
		return encoding;
	}
}
