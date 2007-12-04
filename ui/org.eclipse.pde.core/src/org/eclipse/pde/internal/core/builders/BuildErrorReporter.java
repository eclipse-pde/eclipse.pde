/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - bug 191545
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundleFragmentModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.osgi.framework.Constants;

public class BuildErrorReporter extends ErrorReporter implements IBuildPropertiesConstants {

	private static final String DEF_SOURCE_ENTRY = PROPERTY_SOURCE_PREFIX + '.';

	private class BuildProblem {
		String fEntryToken;
		String fEntryName;
		String fMessage;
		String fCategory;
		int fFixId;
		int fSeverity;
		BuildProblem(String name, String token, String message, int fixId, int severity, String category) {
			fEntryName = name;
			fEntryToken = token;
			fMessage = message;
			fFixId = fixId;
			fSeverity = severity;
			fCategory = category;
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof BuildProblem))
				return false;
			BuildProblem bp = (BuildProblem)obj;
			if (!fEntryName.equals(bp.fEntryName))
				return false;
			if (fEntryToken != null && !fEntryToken.equals(bp.fEntryToken))
				return false;
			if (fFixId != bp.fFixId)
				return false;
			return true;
		}
	}

	class WildcardFilenameFilter implements FilenameFilter {

		private Pattern pattern;

		public WildcardFilenameFilter(String file) {
			pattern = PatternConstructor.createPattern(file, false);
		}

		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
			return matcher.matches();
		}

	}

	private ArrayList fProblemList = new ArrayList();
	private int fBuildSeverity;
	private int fClasspathSeverity;

	public BuildErrorReporter(IFile buildFile) {
		super(buildFile);
	}

	public void validate(IProgressMonitor monitor) {
		fBuildSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD);
		fClasspathSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_UNRESOLVED_IMPORTS);
		if (fBuildSeverity == CompilerFlags.IGNORE && fClasspathSeverity == CompilerFlags.IGNORE)
			return;
		WorkspaceBuildModel wbm = new WorkspaceBuildModel(fFile);
		wbm.load();
		if (!wbm.isLoaded())
			return;
		// check build and store all found errors
		validateBuild(wbm.getBuild(true));

		// if there are any errors report using the text model
		if (fProblemList.size() > 0)
			reportErrors(prepareTextBuildModel(monitor));
	}

	private void validateBuild(IBuild build) {

		IBuildEntry binIncludes = null;
		IBuildEntry binExcludes = null;
		IBuildEntry srcIncludes = null;
		IBuildEntry srcExcludes = null;
		IBuildEntry jarsExtra = null;
		IBuildEntry bundleList = null;
		ArrayList sourceEntries = new ArrayList();
		ArrayList sourceEntryKeys = new ArrayList();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			String name = entries[i].getName();
			if (entries[i].getTokens().length == 0)
				prepareError(name, null,
						PDECoreMessages.BuildErrorReporter_emptyEntry,
						PDEMarkerFactory.B_REMOVAL,
						PDEMarkerFactory.CAT_FATAL);
			else if (name.equals(PROPERTY_BIN_INCLUDES))
				binIncludes = entries[i];
			else if (name.equals(PROPERTY_BIN_EXCLUDES))
				binExcludes = entries[i];
			else if (name.equals(PROPERTY_SRC_INCLUDES))
				srcIncludes = entries[i];
			else if (name.equals(PROPERTY_SRC_EXCLUDES))
				srcExcludes = entries[i];
			else if (name.startsWith(PROPERTY_SOURCE_PREFIX))
				sourceEntries.add(entries[i]);
			else if (name.equals(PROPERTY_JAR_EXTRA_CLASSPATH))
				jarsExtra = entries[i];
			else if (name.equals(IBuildEntry.SECONDARY_DEPENDENCIES))
				bundleList = entries[i];
			else if (name.equals(PROPERTY_CUSTOM)) {
				String[] tokens = entries[i].getTokens();
				if (tokens.length == 1 && tokens[0].equalsIgnoreCase("true")) //$NON-NLS-1$
					// nothing to validate in custom builds
					return;
			}

			// non else if statement to catch all names
			if (name.startsWith(PROPERTY_SOURCE_PREFIX))
				sourceEntryKeys.add(entries[i].getName());
		}

		// validation not relying on build flag
		if (fClasspathSeverity != CompilerFlags.IGNORE) {
			if (jarsExtra != null)
				validateJarsExtraClasspath(jarsExtra);
			if (bundleList != null)
				validateDependencyManagement(bundleList);
		}

		// rest of validation relies on build flag
		if (fBuildSeverity == CompilerFlags.IGNORE)
			return;

		validateIncludes(binIncludes, sourceEntryKeys);
		validateIncludes(binExcludes, sourceEntryKeys);
		validateIncludes(srcIncludes, sourceEntryKeys);
		validateIncludes(srcExcludes, sourceEntryKeys);

		try {
			if (fProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(fProject);
				IClasspathEntry[] cpes = jp.getRawClasspath();
				validateMissingLibraries(sourceEntryKeys, cpes);
				validateSourceEntries(sourceEntries, cpes);
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}

		validateSourceEntries(sourceEntries);
		validateMissingSourceInBinIncludes(binIncludes, sourceEntryKeys, build);
		validateBinIncludes(binIncludes);
	}

	private void validateBinIncludes(IBuildEntry binIncludes) {
		// make sure we have a manifest entry
		if(fProject.exists(ICoreConstants.MANIFEST_PATH)) {
			String key = "META-INF/"; //$NON-NLS-1$
			validateBinIncludes(binIncludes, key);	
		}

		// make sure if we're a fragment, we have a fragment.xml entry
		if(fProject.exists(ICoreConstants.FRAGMENT_PATH)) {
			String key = "fragment.xml"; //$NON-NLS-1$
			validateBinIncludes(binIncludes, key);
		}

		// make sure if we're a plugin, we have a plugin.xml entry
		if(fProject.exists(ICoreConstants.PLUGIN_PATH)) {
			String key = "plugin.xml"; //$NON-NLS-1$
			validateBinIncludes(binIncludes, key);
		}
		
		// validate for bundle localization
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null)
			return;
		if (model instanceof IBundlePluginModelBase && !(model instanceof IBundleFragmentModel)) {
			IBundleModel bm = ((IBundlePluginModelBase)model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_LOCALIZATION);
			if ((mh == null || mh.getValue() == null)) { // check for default location
				Path path = new Path(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
				if(fProject.exists(path))
					validateBinIncludes(binIncludes, Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
			} else { // check for the real location
				String localization = mh.getValue();
				int index = localization.lastIndexOf('/');
				if(index != -1) { // if we're a folder
					String folder = localization.substring(0, index + 1);
					Path path = new Path(folder);
					if(fProject.exists(path))
						validateBinIncludes(binIncludes, folder);
				} else { // if we're just a file location
					String location = mh.getValue().concat(".properties"); //$NON-NLS-1$
					Path path = new Path(location);
					if(fProject.exists(path))
						validateBinIncludes(binIncludes, location);	
				}
			}
		}
		
	}

	private void validateBinIncludes(IBuildEntry binIncludes, String key) {
		if (binIncludes == null)
			return;
		String[] tokens = binIncludes.getTokens();
		boolean exists = false;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].startsWith(key)) {
				exists = true;
				break;
			}

			// check for wildcards
			IPath project = fFile.getProject().getLocation();
			if(project != null && tokens[i] != null) {
				File file = project.toFile();
				File[] files = file.listFiles(new WildcardFilenameFilter(tokens[i]));
				for(int j = 0; j < files.length; j++) {
					if(files[j].toString().endsWith(key)) {
						exists = true;
						break;
					}
				}
			}
		}

		if(!exists) {
			prepareError(PROPERTY_BIN_INCLUDES, 
					key,
					NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key),
					PDEMarkerFactory.B_ADDDITION,
					PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateJarsExtraClasspath(IBuildEntry javaExtra) {
		String platform = "platform:/plugin/";  //$NON-NLS-1$
		String[] tokens = javaExtra.getTokens();
		IPath projectPath = javaExtra.getModel().getUnderlyingResource().getProject().getLocation();
		for (int i = 0; i < tokens.length; i++) {
			boolean exists = true;
			if (tokens[i].startsWith(platform)) {
				String path = tokens[i].substring(platform.length());
				int sep = path.indexOf(IPath.SEPARATOR);
				if (sep > -1) {
					IPluginModelBase model = PluginRegistry.findModel(path.substring(0, sep));
					if (model == null)
						exists = false;
					else {
						IResource resource = model.getUnderlyingResource();
						path = path.substring(sep + 1);
						if (resource == null) {
							String location = model.getInstallLocation();
							File external = new File(location);
							if (external.isDirectory()) {
								IPath p = new Path(location).addTrailingSeparator().append(path);
								exists = new File(p.toOSString()).exists();
							} else
								// compiler will not recognize nested jars, if external location is not
								// a directory this reference "does not exist"
								exists = false;
						} else
							exists = resource.getProject().findMember(path) != null;
					}
				}
			} else
				exists = projectPath.append(tokens[i]).toFile().exists();

			if (!exists && !startsWithAntVariable(tokens[i])) {
				prepareError(PROPERTY_JAR_EXTRA_CLASSPATH, tokens[i],
						NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindJar, tokens[i]),
						PDEMarkerFactory.NO_RESOLUTION, 
						fClasspathSeverity,
						PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	private void validateMissingSourceInBinIncludes(IBuildEntry binIncludes, ArrayList sourceEntryKeys, IBuild build) {
		if (binIncludes == null)
			return;
		for (int i = 0; i < sourceEntryKeys.size(); i++) {
			String key = (String)sourceEntryKeys.get(i);
			// We don't want to flag source.. = . as in  bug 146042 comment 1
			if (DEF_SOURCE_ENTRY.equals(key)) {
				IBuildEntry entry = build.getEntry(DEF_SOURCE_ENTRY);
				String[] tokens = entry.getTokens();
				if (tokens.length == 1 && tokens[0].equals(".")) //$NON-NLS-1$
					continue;
			}
			key = key.substring(PROPERTY_SOURCE_PREFIX.length());
			boolean found = false;
			String[] binIncludesTokens = binIncludes.getTokens();
			for (int j = 0; j < binIncludesTokens.length; j++) {
				Pattern pattern = PatternConstructor.createPattern(binIncludesTokens[j], false);
				if (pattern.matcher(key).matches())
					found = true;
			}
			if (!found)
				prepareError(PROPERTY_BIN_INCLUDES, key,
						NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key),
						PDEMarkerFactory.B_ADDDITION,
						PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateSourceEntries(ArrayList sourceEntries) {
		for (int i = 0; i < sourceEntries.size(); i++) {
			String name = ((IBuildEntry)sourceEntries.get(i)).getName();
			String[] tokens = ((IBuildEntry)sourceEntries.get(i)).getTokens();
			for (int j = 0; j < tokens.length; j++) {
				if (".".equals(tokens[j])) //$NON-NLS-1$
					continue;
				IResource folderEntry = fProject.findMember(tokens[j]);
				if (folderEntry == null 
						|| !folderEntry.exists() 
						|| !(folderEntry instanceof IFolder))
					prepareError(name, tokens[j],
							NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, tokens[j]),
							PDEMarkerFactory.B_REMOVAL,
							PDEMarkerFactory.CAT_OTHER);
			}

		}
	}

	private void validateMissingLibraries(ArrayList sourceEntryKeys, IClasspathEntry[] cpes) {
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null)
			return;
		if (model instanceof IBundlePluginModelBase && !(model instanceof IBundleFragmentModel)) {
			IBundleModel bm = ((IBundlePluginModelBase)model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if ((mh == null || mh.getValue() == null)) {
				for (int i = 0; i < cpes.length; i++) {
					if (cpes[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY))
							prepareError(DEF_SOURCE_ENTRY, null,
									PDECoreMessages.BuildErrorReporter_sourceMissing,
									PDEMarkerFactory.NO_RESOLUTION,
									PDEMarkerFactory.CAT_OTHER);
						break;
					}
				}
			}
		}
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			String libname = libraries[i].getName();
			if (libname.equals(".")) { //$NON-NLS-1$
				// no need to flag anything if the project contains no source folders.
				for (int j = 0; j < cpes.length; j++) {
					if (cpes[j].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY))
							prepareError(DEF_SOURCE_ENTRY, null,
									PDECoreMessages.BuildErrorReporter_sourceMissing,
									PDEMarkerFactory.NO_RESOLUTION,
									PDEMarkerFactory.CAT_OTHER);
						break;
					}
				}
				continue;
			} else if (fProject.findMember(libname) != null) {//$NON-NLS-1$
				// non "." library entries that exist in the workspace
				// don't have to be referenced in the build properties
				continue;
			}
			String sourceEntryKey = PROPERTY_SOURCE_PREFIX + libname;
			if (!sourceEntryKeys.contains(sourceEntryKey) 
					&& !containedInFragment(model.getBundleDescription(), libname))
				prepareError(sourceEntryKey, null,
						NLS.bind(PDECoreMessages.BuildErrorReporter_missingEntry, sourceEntryKey),
						PDEMarkerFactory.B_SOURCE_ADDITION,
						PDEMarkerFactory.CAT_OTHER);
		}
	}

	private boolean containedInFragment(BundleDescription description, String libname) {
		if(description == null)
			return false;

		BundleDescription[] fragments = description.getFragments();

		if (fragments == null)
			return false;
		for (int j = 0; j < fragments.length; j++) {
			IPluginModelBase fragmentModel = PluginRegistry.findModel(fragments[j]);
			if (fragmentModel != null && fragmentModel.getUnderlyingResource() != null) {
				IProject project = fragmentModel.getUnderlyingResource().getProject();
				if (project.findMember(libname) != null)
					return true;
				try {
					IBuild build = ClasspathUtilCore.getBuild(fragmentModel);
					if (build != null) {
						IBuildEntry[] entries = build.getBuildEntries();
						for (int i = 0; i < entries.length; i++)
							if (entries[i].getName().equals(PROPERTY_SOURCE_PREFIX + libname))
								return true;
						return false;
					}
				} catch (CoreException e) {
				}
			} else {
				String location = fragments[j].getLocation();
				File external = new File(location);
				if (external.exists()) {
					if (external.isDirectory()) {
						IPath p = new Path(location).addTrailingSeparator().append(libname);
						return new File(p.toOSString()).exists();
					}
					return CoreUtility.jarContainsResource(external, libname, false);
				}
			}
		}
		return false;
	}

	private void validateSourceEntries(ArrayList sourceEntries, IClasspathEntry[] cpes) {
		String[] unlisted = PDEBuilderHelper.getUnlistedClasspaths(sourceEntries, fProject, cpes);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < unlisted.length; i++) {
			if (unlisted[i] == null)
				break;
			if (sb.length() > 0)
				sb.append(", "); //$NON-NLS-1$
			sb.append(unlisted[i]);
		}
		String unlistedEntries = sb.toString();
		if (sb.length() == 0)
			return;
		if (sourceEntries.size() == 1) {
			String name = ((IBuildEntry)sourceEntries.get(0)).getName();
			prepareError(name, null,
					NLS.bind(PDECoreMessages.BuildErrorReporter_classpathEntryMissing1, unlistedEntries, name),
					PDEMarkerFactory.B_SOURCE_ADDITION,
					PDEMarkerFactory.CAT_OTHER);
		} else
			prepareError(DEF_SOURCE_ENTRY, null,
					NLS.bind(PDECoreMessages.BuildErrorReporter_classpathEntryMissing, unlistedEntries),
					PDEMarkerFactory.B_SOURCE_ADDITION,
					PDEMarkerFactory.CAT_OTHER);

	}

	private void validateIncludes(IBuildEntry includes, ArrayList sourceIncludes) {
		if (includes == null)
			return;
		String[] tokens = includes.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].trim();
			if (token.indexOf("*") != -1) //$NON-NLS-1$
				// skip entries with wildcards
				continue;
			if (token.equals(".")) //$NON-NLS-1$
				// skip . since we know it exists
				continue;
			if (startsWithAntVariable(token))
				// skip '${x}' variables
				continue;
			IResource member = fProject.findMember(token);
			String message = null;
			int fixId = PDEMarkerFactory.NO_RESOLUTION;
			if (member == null) {
				if (sourceIncludes.contains(PROPERTY_SOURCE_PREFIX + token))
					continue;
				if (token.endsWith("/")) //$NON-NLS-1$
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, token);
				else
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFile, token);
				fixId = PDEMarkerFactory.B_REMOVAL;
			} else if (token.endsWith("/") && !(member instanceof IFolder)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_entiresMustRefDirs, token);
				fixId = PDEMarkerFactory.B_REMOVE_SLASH_FILE_ENTRY;
			} else if (!token.endsWith("/") && !(member instanceof IFile)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_dirsMustEndSlash, token);
				fixId = PDEMarkerFactory.B_APPEND_SLASH_FOLDER_ENTRY;
			}

			if (message != null)
				prepareError(includes.getName(), token, message, fixId, PDEMarkerFactory.CAT_OTHER);
		}
	}
	
	private boolean startsWithAntVariable(String token) {
		int varStart = token.indexOf("${"); //$NON-NLS-1$
		return varStart != -1 && varStart < token.indexOf("}"); //$NON-NLS-1$
	}

	private void validateDependencyManagement(IBuildEntry bundleList) {
		String[] bundles = bundleList.getTokens();
		for (int i = 0; i < bundles.length; i++) {
			if (PluginRegistry.findModel(bundles[i]) == null)
				prepareError(IBuildEntry.SECONDARY_DEPENDENCIES, bundles[i], 
						NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindBundle, bundles[i]), 
						PDEMarkerFactory.NO_RESOLUTION, fClasspathSeverity, PDEMarkerFactory.CAT_OTHER);
		}

	}

	private BuildModel prepareTextBuildModel(IProgressMonitor monitor)  {
		try {
			IDocument doc = createDocument(fFile);
			if (doc == null)
				return null;
			BuildModel bm = new BuildModel(doc, true);
			bm.load();
			if (!bm.isLoaded())
				return null;
			return bm;
		} catch (CoreException e) {
			PDECore.log(e);
			return null;
		}
	}

	private void reportErrors(BuildModel bm) {
		if (bm == null)
			return;

		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem bp = (BuildProblem)fProblemList.get(i);

			int lineNum;
			IBuildEntry buildEntry = bm.getBuild().getEntry(bp.fEntryName);
			if (buildEntry == null || bp.fEntryName == null)
				// general file case (eg. missing source.* entry)
				lineNum = 1;
			else
				// issue with a particular entry
				lineNum = getLineNumber(buildEntry, bp.fEntryToken);

			if (lineNum > 0)
				report(bp.fMessage, lineNum, bp.fFixId, bp.fEntryName, bp.fEntryToken, bp.fSeverity, bp.fCategory);
		}
	}

	private int getLineNumber(IBuildEntry ibe, String tokenString) {
		if (!(ibe instanceof BuildEntry))
			return 0;
		BuildEntry be = (BuildEntry)ibe;
		IDocument doc = ((BuildModel)be.getModel()).getDocument();
		try {
			int buildEntryLineNumber = doc.getLineOfOffset(be.getOffset()) + 1;
			if (tokenString == null)
				// we are interested in the build entry name
				// (getLineOfOffset is 0-indexed, need 1-indexed)
				return buildEntryLineNumber;

			// extract the full entry
			String entry = doc.get(be.getOffset(), be.getLength());

			int valueIndex = entry.indexOf('=') + 1;
			if (valueIndex == 0 || valueIndex == entry.length())
				return buildEntryLineNumber;

			// remove the entry name			
			entry = entry.substring(valueIndex);

			int entryTokenOffset = entry.indexOf(tokenString);
			if (entryTokenOffset == -1)
				return buildEntryLineNumber;

			// skip ahead to 1st occurence
			entry = entry.substring(entryTokenOffset);
			int currOffset = be.getOffset() + valueIndex + entryTokenOffset;
			while (true) {
				// tokenize string using ',' as a delimiter, trim out whitespace and '\' characters
				// during comparison
				if (entry.charAt(0) == '\\') {
					currOffset++;
					entry = entry.substring(1);
				}
				int cci = entry.indexOf(',');
				if (cci == -1) {
					if (entry.trim().equals(tokenString))
						return doc.getLineOfOffset(currOffset + entry.indexOf(tokenString)) + 1;
					return buildEntryLineNumber;
				}

				String ct = entry.substring(0, cci).trim();
				if (ct.equals(tokenString)) 
					return doc.getLineOfOffset(currOffset) + 1;

				entry = entry.substring(++cci);
				currOffset += cci;
			}

		} catch (BadLocationException e) {
		}
		return 0;
	}

	private void prepareError(String name, String token, String message, int fixId, String category) {
		prepareError(name, token, message, fixId, fBuildSeverity, category);
	}

	private void prepareError(String name, String token, String message, int fixId, int severity, String category) {
		BuildProblem bp = new BuildProblem(name, token, message, fixId, severity, category);
		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem listed = (BuildProblem)fProblemList.get(i);
			if (listed.equals(bp))
				return;
		}
		fProblemList.add(bp);
	}

	private void report(String message, int line, int problemID, String buildEntry, String buildToken, int severity, String category) {
		IMarker marker = report(message, line, severity, problemID, category);
		if (marker == null)
			return;
		try {
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_ENTRY, buildEntry);
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_TOKEN, buildToken);
		} catch (CoreException e) {
		}
	}

}
