package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.util.ArrayList;
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
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PluginModelManager;
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

public class BuildErrorReporter extends ErrorReporter {
	
	private static final int NO_RES = PDEMarkerFactory.NO_RESOLUTION;
	private static final String BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	private static final String SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	private static final String CUSTOM = "custom"; //$NON-NLS-1$
	public static final String SOURCE = "source."; //$NON-NLS-1$
	private static final String DEF_SOURCE_ENTRY = SOURCE + '.';
	
	private class BuildProblem {
		String fEntryToken;
		String fEntryName;
		String fMessage;
		int fFixId;
		BuildProblem(String name, String token, String message, int fixId) {
			fEntryName = name;
			fEntryToken = token;
			fMessage = message;
			fFixId = fixId;
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
	
	private ArrayList fProblemList = new ArrayList();
	private int fSeverity;
	
	public BuildErrorReporter(IFile buildFile, int severity) {
		super(buildFile);
		fSeverity = severity;
	}

	public void validate(IProgressMonitor monitor) {
		if (fSeverity == CompilerFlags.IGNORE)
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
		IBuildEntry srcIncludes = null;
		ArrayList sourceEntries = new ArrayList();
		ArrayList sourceEntryKeys = new ArrayList();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			String name = entries[i].getName();
			if (entries[i].getTokens().length == 0) {
				prepareError(name, null,
						PDECoreMessages.BuildErrorReporter_emptyEntry,
						PDEMarkerFactory.B_REMOVAL);
			} else if (name.equals(BIN_INCLUDES))
				binIncludes = entries[i];
			else if (name.equals(SRC_INCLUDES))
				srcIncludes = entries[i];
			else if (name.startsWith(SOURCE)) {
				sourceEntries.add(entries[i]);
				sourceEntryKeys.add(entries[i].getName());
			} else if (name.equals(CUSTOM)) {
				String[] tokens = entries[i].getTokens();
				if (tokens.length == 1 && tokens[0].equalsIgnoreCase("true")) //$NON-NLS-1$
					// nothing to validate in custom builds
					return;
			}	
		}
		
		validateIncludes(binIncludes, sourceEntryKeys);
		validateIncludes(srcIncludes, sourceEntryKeys);
		
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
		validateMissingSourceInBinIncludes(binIncludes, sourceEntryKeys);
		
	}
	
	private void validateMissingSourceInBinIncludes(IBuildEntry binIncludes, ArrayList sourceEntryKeys) {
		if (binIncludes == null)
			return;
		for (int i = 0; i < sourceEntryKeys.size(); i++) {
			String key = (String)sourceEntryKeys.get(i);
			key = key.substring(SOURCE.length());
			boolean found = false;
			String[] binIncludesTokens = binIncludes.getTokens();
			for (int j = 0; j < binIncludesTokens.length; j++) {
				Pattern pattern = PatternConstructor.createPattern(binIncludesTokens[j], false);
				if (pattern.matcher(key).matches())
					found = true;
			}
			if (!found)
				prepareError(BIN_INCLUDES, key,
						NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key),
						PDEMarkerFactory.B_ADDDITION);
		}
	}

	private void validateSourceEntries(ArrayList sourceEntries) {
		for (int i = 0; i < sourceEntries.size(); i++) {
			String name = ((IBuildEntry)sourceEntries.get(i)).getName();
			String[] tokens = ((IBuildEntry)sourceEntries.get(i)).getTokens();
			for (int j = 0; j < tokens.length; j++) {
				IResource folderEntry = fProject.findMember(tokens[j]);
				if (folderEntry == null 
						|| !folderEntry.exists() 
						|| !(folderEntry instanceof IFolder))
					prepareError(name, tokens[j],
							NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, tokens[j]),
							PDEMarkerFactory.B_REMOVAL);
			}
			
		}
	}

	private void validateMissingLibraries(ArrayList sourceEntryKeys, IClasspathEntry[] cpes) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(fProject);
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
									PDEMarkerFactory.B_SOURCE_ADDITION);
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
									PDEMarkerFactory.B_SOURCE_ADDITION);
						break;
					}
				}
				continue;
			} else if (fProject.findMember(libname) != null) {//$NON-NLS-1$
				// non "." library entries that exist in the workspace
				// don't have to be referenced in the build properties
				continue;
			}
			String sourceEntryKey = SOURCE + libname;
			if (!sourceEntryKeys.contains(sourceEntryKey) 
					&& !containedInFragment(manager, model.getBundleDescription().getFragments(), libname))
				prepareError(sourceEntryKey, null,
						NLS.bind(PDECoreMessages.BuildErrorReporter_missingEntry, sourceEntryKey),
						PDEMarkerFactory.B_SOURCE_ADDITION);
		}
	}

	private boolean containedInFragment(PluginModelManager manager, BundleDescription[] fragments, String libname) {
		if (fragments == null)
			return false;
		for (int j = 0; j < fragments.length; j++) {
			ModelEntry entry = manager.findEntry(fragments[j].getSymbolicName());
			IPluginModelBase fragmentModel = entry.getWorkspaceModel();
			if (fragmentModel != null) {
				IProject project = fragmentModel.getUnderlyingResource().getProject();
				if (project.findMember(libname) != null)
					return true;
				try {
					IBuild build = ClasspathUtilCore.getBuild(fragmentModel);
					if (build != null) {
						IBuildEntry[] entries = build.getBuildEntries();
						for (int i = 0; i < entries.length; i++)
							if (entries[i].getName().equals(SOURCE + libname))
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
		String[] unlisted = getUnlistedClasspaths(sourceEntries, fProject, cpes);
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
					PDEMarkerFactory.B_SOURCE_ADDITION);
		} else
			prepareError(DEF_SOURCE_ENTRY, null,
					NLS.bind(PDECoreMessages.BuildErrorReporter_classpathEntryMissing, unlistedEntries),
					PDEMarkerFactory.B_SOURCE_ADDITION);
		
	}
	
	public static String[] getUnlistedClasspaths(ArrayList sourceEntries, IProject project, IClasspathEntry[] cpes) {
		String[] unlisted = new String[cpes.length];
		int index = 0;
		for (int i = 0; i < cpes.length; i++) {
			if (cpes[i].getEntryKind() != IClasspathEntry.CPE_SOURCE)
				continue;
			IPath path = cpes[i].getPath();
			boolean found = false;
			for (int j = 0; j < sourceEntries.size(); j++) {
				IBuildEntry be = (IBuildEntry)sourceEntries.get(j);
				String[] tokens = be.getTokens();
				for (int k = 0; k < tokens.length; k++) {
					IResource res = project.findMember(tokens[k]);
					if (res == null)
						continue;
					IPath ipath = res.getFullPath();
					if (ipath.equals(path))
						found = true;
				}
			}
			if (!found)
				unlisted[index++] = path.removeFirstSegments(1).addTrailingSeparator().toString();
		}
		return unlisted;
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
			int varStart = token.indexOf("${"); //$NON-NLS-1$
			if (varStart != -1 && varStart < token.indexOf("}")) //$NON-NLS-1$
				// skip '${x}' variables
				continue;
			IResource member = fProject.findMember(token);
			String message = null;
			int fixId = NO_RES;
			if (member == null) {
				if (sourceIncludes.contains(SOURCE + token))
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
				prepareError(includes.getName(), token, message, fixId);
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
				report(bp.fMessage, lineNum, bp.fFixId, bp.fEntryName, bp.fEntryToken);
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
	
	private void prepareError(String name, String token, String message, int fixId) {
		BuildProblem bp = new BuildProblem(name, token, message, fixId);
		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem listed = (BuildProblem)fProblemList.get(i);
			if (listed.equals(bp))
				return;
		}
		fProblemList.add(bp);
	}
	
	private void report(String message, int line, int problemID, String buildEntry, String buildToken) {
		IMarker marker = report(message, line, fSeverity, problemID);
		if (marker == null)
			return;
		try {
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_ENTRY, buildEntry);
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_TOKEN, buildToken);
		} catch (CoreException e) {
		}
	}
 }
