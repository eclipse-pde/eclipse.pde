package org.eclipse.pde.internal.builders;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.PDEMessages;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.osgi.framework.Constants;

public class BuildErrorReporter extends ErrorReporter {
	
	private static final String BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	private static final String SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	private static final String SOURCE = "source."; //$NON-NLS-1$
	private static final String CUSTOM = "custom"; //$NON-NLS-1$
	private static final String TEMPSTR1 = PDEMessages.BuildErrorReporter_missingEntry;
	private static final String TEMPSTR2 = PDEMessages.BuildErrorReporter_missingFolder;
	
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
				prepareError(name, null, PDEMessages.BuildErrorReporter_emptyEntry);
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
		
		validateMissingIncludes(binIncludes, srcIncludes);
		validateIncludes(binIncludes);
		validateIncludes(srcIncludes);
		
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

	private void validateMissingIncludes(IBuildEntry binIncludes, IBuildEntry srcIncludes) {
		if (binIncludes == null)
			prepareError(NLS.bind(TEMPSTR1, BIN_INCLUDES));
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
				if (key.equals(binIncludesTokens[j]))
					found = true;
			}
			if (!found)
				prepareError(BIN_INCLUDES, null, NLS.bind(PDEMessages.BuildErrorReporter_binIncludesMissing, key));
		}
	}

	private void validateSourceEntries(ArrayList sourceEntries) {
		for (int i = 0; i < sourceEntries.size(); i++) {
			String name = ((IBuildEntry)sourceEntries.get(i)).getName();
			String[] tokens = ((IBuildEntry)sourceEntries.get(i)).getTokens();
			for (int j = 0; j < tokens.length; j++) {
				if (!tokens[j].endsWith("/")) //$NON-NLS-1$
					prepareError(name, tokens[j], NLS.bind(PDEMessages.BuildErrorReporter_sourceTokens, tokens[j]));
				else {
					IResource folderEntry = fProject.findMember(tokens[j]);
					if (folderEntry == null 
							|| !folderEntry.exists() 
							|| !(folderEntry instanceof IFolder))
						prepareError(name, tokens[j], NLS.bind(TEMPSTR2, tokens[j]));
				}
			}
			
		}
	}

	private void validateMissingLibraries(ArrayList sourceEntryKeys, IClasspathEntry[] cpes) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(fProject);
		if (model == null)
			return;
		if (model instanceof IBundlePluginModelBase) {
			IBundleModel bm = ((IBundlePluginModelBase)model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if ((mh == null || mh.getValue() == null)) {
				for (int i = 0; i < cpes.length; i++) {
					if (cpes[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (!sourceEntryKeys.contains(SOURCE + ".")) //$NON-NLS-1$
							prepareError(PDEMessages.BuildErrorReporter_sourceMissing);
						break;
					}
				}
			}
		}
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			String libname = libraries[i].getName();
			if (!libname.equals(".") &&	fProject.findMember(libname) != null) //$NON-NLS-1$
				// non "." library entries that exist in the workspace
				// don't have to be referenced in the build properties
				continue;
			String buildEntryKey = SOURCE + libname;
			if (!sourceEntryKeys.contains(buildEntryKey))
				prepareError(NLS.bind(TEMPSTR1, buildEntryKey));
		}
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
				String spath = path.removeFirstSegments(i).addTrailingSeparator().toString();
				if (sourceEntries.size() == 1) {
					String name = ((IBuildEntry)sourceEntries.get(0)).getName();
					prepareError(name, null, NLS.bind(PDEMessages.BuildErrorReporter_classpathEntryMissing1, spath, name));
				} else
					prepareError(NLS.bind(PDEMessages.BuildErrorReporter_classpathEntryMissing, spath));
			}
		}
	}
	
	private void validateIncludes(IBuildEntry includes) {
		if (includes == null)
			return;
		String[] tokens = includes.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].trim();
			IResource member = fProject.findMember(token);
			String message = null;
			if (member == null) {
				if (token.endsWith("/")) //$NON-NLS-1$
					message = NLS.bind(TEMPSTR2, token);
				else
					message = NLS.bind(PDEMessages.BuildErrorReporter_missingFile, token);
			} else if (token.equals(".")) //$NON-NLS-1$
				// skip . since it retuns an IFolder
				continue;
			else if (token.indexOf("*") != -1) //$NON-NLS-1$
				// skip wildcards
				continue;
			else if (token.endsWith("/") && !(member instanceof IFolder)) //$NON-NLS-1$
				message = NLS.bind(PDEMessages.BuildErrorReporter_entiresMustRefDirs, token);
			else if (!token.endsWith("/") && !(member instanceof IFile)) //$NON-NLS-1$
				message = NLS.bind(PDEMessages.BuildErrorReporter_dirsMustEndSlash, token);
			
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
			// general file case (eg. missing entry)
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
			return -1;
		BuildEntry be = (BuildEntry)ibe;
		IDocument doc = ((BuildModel)be.getModel()).getDocument();
		try {
			if (tokenString == null)
				// we are interested in the build entry name
				// (getLineOfOffset is 0-indexed, need 1-indexed)
				return doc.getLineOfOffset(be.getOffset()) + 1;

			// extract the full entry
			String entry = doc.get(be.getOffset(), be.getLength());
			
			int index = entry.indexOf('=') + 1;
			if (index == 0 || index == entry.length())
				return -1;
			
			// remove the entry name			
			entry = entry.substring(index);
			
			int entryTokenOffset = entry.indexOf(tokenString);
			if (entryTokenOffset == -1)
				return -1;
			
			// check to see if single occurence
			if (entryTokenOffset == entry.lastIndexOf(tokenString))
				return doc.getLineOfOffset(be.getOffset() + index + entryTokenOffset) + 1;
			
			// multiple occurences, must comb through to find exact location
			entryTokenOffset = 0;
			while (true) {
				// tokenize string using ',' as a delimiter, trim out whitespace and '\' characters
				// during comparison
				int cci = entry.indexOf(',');
				if (cci == -1 && entry.indexOf(tokenString) == -1)
					return -1;
				
				// if entry starts with slash make sure token does not include it
				boolean sws = entry.charAt(0) == '\\'; 
				String ct = entry.substring(sws ? 1 : 0, cci);
				entryTokenOffset += cci;
				if (ct.trim().equals(tokenString))
					return doc.getLineOfOffset(be.getOffset() + index + entryTokenOffset) + 1;
				
				entry = entry.substring(cci + 1);
			}
			
		} catch (BadLocationException e) {
		}
		return -1;
	}
	
	private void prepareError(String name, String token, String message) {
		BuildProblem bp = new BuildProblem(name, token, message);
		fProblemList.add(bp);
	}
	
	private void prepareError(String message) {
		prepareError(null, null, message);
	}
 }
