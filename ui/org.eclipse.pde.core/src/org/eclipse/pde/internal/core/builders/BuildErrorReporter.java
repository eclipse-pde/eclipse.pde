package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
	
	private static final String BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	private static final String SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	private static final String SOURCE = "source."; //$NON-NLS-1$
	private static final String CUSTOM = "custom"; //$NON-NLS-1$
	
	private class BuildProblem {
		IBuildEntry fBuildEntry;
		String fEntryToken;
		String fEntryName;
		String fMessage;
		BuildProblem(String name, String token, String message) {
			fEntryName = name;
			fEntryToken = token;
			fMessage = message;
		}
		boolean noLine() {
			return fEntryName == null;
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
			reportErrors(prepareTextBuildModel(monitor), monitor);
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
				prepareError(name, null, PDECoreMessages.BuildErrorReporter_emptyEntry);
				continue;
			}
			if (name.equals(BIN_INCLUDES))
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
				prepareError(BIN_INCLUDES, null, NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key));
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
					prepareError(name, tokens[j], NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, tokens[j]));
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
						if (!sourceEntryKeys.contains(SOURCE + ".")) //$NON-NLS-1$
							prepareError(PDECoreMessages.BuildErrorReporter_sourceMissing);
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
						if (!sourceEntryKeys.contains(SOURCE + ".")) //$NON-NLS-1$
							prepareError(PDECoreMessages.BuildErrorReporter_sourceMissing);
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
				prepareError(NLS.bind(PDECoreMessages.BuildErrorReporter_missingEntry, sourceEntryKey));
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
						for (int i = 0; i < entries.length; i++) {
							if (entries[i].getName().equals(SOURCE + libname))
								return true;
						}
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
		for (int i = 0; i < cpes.length; i++) {
			if (cpes[i].getEntryKind() != IClasspathEntry.CPE_SOURCE)
				continue;
			IPath path = cpes[i].getPath();
			boolean found = false;
			for (int j = 0; j < sourceEntries.size(); j++) {
				IBuildEntry be = (IBuildEntry)sourceEntries.get(j);
				String[] tokens = be.getTokens();
				for (int k = 0; k < tokens.length; k++) {
					IResource res = fProject.findMember(tokens[k]);
					if (res == null)
						continue;
					IPath ipath = res.getFullPath();
					if (ipath.equals(path))
						found = true;
				}
			}
			if (!found) {
				String spath = path.removeFirstSegments(1).addTrailingSeparator().toString();
				if (sourceEntries.size() == 1) {
					String name = ((IBuildEntry)sourceEntries.get(0)).getName();
					prepareError(name, null, NLS.bind(PDECoreMessages.BuildErrorReporter_classpathEntryMissing1, spath, name));
				} else
					prepareError(NLS.bind(PDECoreMessages.BuildErrorReporter_classpathEntryMissing, spath));
			}
		}
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
			if (member == null) {
				if (sourceIncludes.contains(SOURCE + token))
					continue;
				if (token.endsWith("/")) //$NON-NLS-1$
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, token);
				else
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFile, token);
			} else if (token.endsWith("/") && !(member instanceof IFolder)) //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_entiresMustRefDirs, token);
			else if (!token.endsWith("/") && !(member instanceof IFile)) //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_dirsMustEndSlash, token);
			
			if (message != null)
				prepareError(includes.getName(), token, message);
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
			return null;
		}
	}
	
	private void reportErrors(BuildModel bm, IProgressMonitor monitor) {
		if (bm == null)
			return;
		IBuild build = bm.getBuild();
		IBuildEntry[] buildEntries = build.getBuildEntries();
		HashMap entries = new HashMap();
		for (int i = 0; i < buildEntries.length; i++) {
			entries.put(buildEntries[i].getName(), buildEntries[i]);	
		}
		
		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem bp = (BuildProblem)fProblemList.get(i);
			if (bp == null)
				continue;
			
			int lineNum;
			// general file case (eg. missing source.* entry)
			if (bp.noLine())
				lineNum = 1;
			// issue with a particular entry
			else {
				bp.fBuildEntry = (IBuildEntry)entries.get(bp.fEntryName);
				if (bp.fBuildEntry == null)
					continue;
				lineNum = getLineNumber(bp.fBuildEntry, bp.fEntryToken);
			}
			if (lineNum > 0)
				report(bp.fMessage, lineNum, fSeverity);
		}
	}
	
	private int getLineNumber(IBuildEntry ibe, String tokenString) {
		if (!(ibe instanceof BuildEntry))
			return 0;
		BuildEntry be = (BuildEntry)ibe;
		IDocument doc = ((BuildModel)be.getModel()).getDocument();
		try {
			if (tokenString == null)
				// we are interested in the build entry name
				// (getLineOfOffset is 0-indexed, need 1-indexed)
				return doc.getLineOfOffset(be.getOffset()) + 1;

			// extract the full entry
			String entry = doc.get(be.getOffset(), be.getLength());
			
			int valueIndex = entry.indexOf('=') + 1;
			if (valueIndex == 0 || valueIndex == entry.length())
				return 0;
			
			// remove the entry name			
			entry = entry.substring(valueIndex);
			
			int entryTokenOffset = entry.indexOf(tokenString);
			if (entryTokenOffset == -1)
				return 0;
			
			// check to see if single occurence
			if (entryTokenOffset == entry.lastIndexOf(tokenString))
				return doc.getLineOfOffset(be.getOffset() + valueIndex + entryTokenOffset) + 1;
			
			// multiple occurences, must comb through to find exact location
			entryTokenOffset = 0;
			while (true) {
				// tokenize string using ',' as a delimiter, trim out whitespace and '\' characters
				// during comparison
				int cci = entry.indexOf(',');
				if (cci == -1 && entry.indexOf(tokenString) == -1)
					return 0;
				
				// if entry starts with slash make sure token does not include it
				boolean sws = entry.charAt(0) == '\\'; 
				String ct = entry.substring(sws ? 1 : 0, cci);
				entryTokenOffset += cci;
				if (ct.trim().equals(tokenString))
					return doc.getLineOfOffset(be.getOffset() + valueIndex + entryTokenOffset) + 1;
				
				entry = entry.substring(cci + 1);
			}
			
		} catch (BadLocationException e) {
		}
		return 0;
	}
	
	private void prepareError(String name, String token, String message) {
		BuildProblem bp = new BuildProblem(name, token, message);
		fProblemList.add(bp);
	}
	
	private void prepareError(String message) {
		prepareError(null, null, message);
	}
 }
