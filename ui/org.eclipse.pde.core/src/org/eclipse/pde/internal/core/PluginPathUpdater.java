package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;

public class PluginPathUpdater {
	public static final String KEY_UPDATING = "PluginPathUpdater.updating";
	public static final String JRE_VAR = "JRE_LIB";
	public static final String JRE_SRCVAR = "JRE_SRC";
	public static final String JRE_SRCROOTVAR = "JRE_SRCROOT";
	private Iterator checkedPlugins;
	private boolean relative=true;

	public static class PluginEntry {
		private IPlugin plugin;
		private boolean exported;
		public PluginEntry(IPlugin plugin) {
			this.plugin = plugin;
		}
		public IPlugin getPlugin() {
			return plugin;
		}
		public void setExported(boolean exported) {
			this.exported = exported;
		}
		public boolean isExported() {
			return exported;
		}
	}

	public PluginPathUpdater(Iterator checkedPlugins) {
		this.checkedPlugins = checkedPlugins;
	}
	public PluginPathUpdater(Iterator checkedPlugins, boolean relative) {
		this(checkedPlugins);
		this.relative = relative;
	}
	private void addFoldersToClasspathEntries(
		IPluginModelBase model,
		Vector result) {
		IFile file = (IFile) model.getUnderlyingResource();
		IPath buildPath =
			file.getProject().getFullPath().append("build.properties");
		IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
		if (!buildFile.exists())
			return;
		WorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		manager.connect(buildFile, null, false);
		IBuildModel buildModel =
			(IBuildModel) manager.getModel(buildFile, null);
		IBuild build = buildModel.getBuild();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			IBuildEntry entry = entries[i];
			if (!entry.getName().startsWith("source."))
				continue;
			String[] tokens = entry.getTokens();
			for (int j = 0; j < tokens.length; j++) {
				String folderName = tokens[j];
				IPath folderPath =
					file.getProject().getFullPath().append(folderName);
				if (file.getWorkspace().getRoot().exists(folderPath)) {
					result.add(JavaCore.newSourceEntry(folderPath));
				}
			}
		}
		manager.disconnect(buildFile, null);
	}
	private static void addToClasspathEntries(
		PluginEntry element,
		boolean relative,
		boolean firstLevel,
		Vector result) {
		IPlugin plugin = element.getPlugin();
		IPluginModelBase model = plugin.getModel();
		boolean internal = model.getUnderlyingResource() != null;

		if (internal) {
			try {
				IPath projectPath =
					model.getUnderlyingResource().getProject().getFullPath();
				if (model.getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID) &&
					!isEntryAdded(projectPath,
					IClasspathEntry.CPE_PROJECT,
					result)) {
					IClasspathEntry projectEntry =
						JavaCore.newProjectEntry(projectPath, element.isExported());
					result.addElement(projectEntry);
				}
				// Since fragments are not in the parent plug-in's classpath (to avoid cycles), add
				// fragments which contain "source" to the classpath of the project directly.
				addWorkspaceFragmentContributions(model,result);
			} catch (JavaModelException e) {
			} catch (CoreException e) {
			}
			return;
		}
		IPath modelPath;
		
		if (relative) modelPath = ((ExternalPluginModelBase)model).getEclipseHomeRelativePath();
		else modelPath = new Path(model.getInstallLocation());

		IPluginLibrary[] libraries = plugin.getLibraries();

		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];

			String name = expandLibraryName(library.getName());
			IPath libraryPath = modelPath.append(name);
			IPath[] sourceAnnot = getSourceAnnotation(plugin, modelPath, name, relative);
			int entryKind = relative ? IClasspathEntry.CPE_VARIABLE : IClasspathEntry.CPE_LIBRARY;
			if (!isEntryAdded(libraryPath,
				entryKind,
				result)) {
				boolean exported = (firstLevel && element.isExported() && library.isExported());
				IClasspathEntry libraryEntry =
					relative? 
					JavaCore.newVariableEntry(
						libraryPath,
						sourceAnnot[0],
						sourceAnnot[1],exported) :
					JavaCore.newLibraryEntry(
						libraryPath,
						sourceAnnot[0],
						sourceAnnot[1],exported);
				IClasspathEntry resolved =
					relative ? 
					JavaCore.getResolvedClasspathEntry(libraryEntry) : libraryEntry;
				if (resolved != null && resolved.getPath().toFile().exists())
					result.addElement(libraryEntry);
				else if (!(model instanceof IFragmentModel)) {
					// cannot find this entry - try to locate it 
					// in one of the fragments
					libraryEntry =
						getFragmentEntry(
							(IPluginModel) model,
							name,
							relative,
							element.isExported());
					if (libraryEntry != null
						&& !isEntryAdded(libraryEntry.getPath(),
							entryKind,
							result)) {
						result.addElement(libraryEntry);
					}
				}
			}
		}
		// Recursively add reexported libraries
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			// don't recurse if the library is not reexported
			if (iimport.isReexported() == false)
				continue;
			String id = iimport.getId();
			IPlugin reference = PDECore.getDefault().findPlugin(id);
			if (reference != null) {
				PluginEntry ref = new PluginEntry(reference);
				addToClasspathEntries(ref, relative, false, result);
			}
		}
	}

	private static void addWorkspaceFragmentContributions(IPluginModelBase model, Vector result) {
		try {
			IFragment[] fragments = PDECore.getDefault().getWorkspaceModelManager().getFragmentsFor(model.getPluginBase().getId(),model.getPluginBase().getVersion());
			for (int i = 0; i < fragments.length; i++) {
				IProject project = fragments[i].getModel().getUnderlyingResource().getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProject = JavaCore.create(project);
					IClasspathEntry[] entries = jProject.getRawClasspath();
					for (int j = 0; j < entries.length; j++) {
						if (entries[j].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							IClasspathEntry projectEntry =
								JavaCore.newProjectEntry(project.getFullPath(), true);
							result.addElement(projectEntry);
							break;
						}
					}
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
	}

	private static IClasspathEntry getFragmentEntry(
		IPluginModel model,
		String name,
		boolean relative,
		boolean exported) {
		IFragmentModel[] fragments =
			PDECore.getDefault().getExternalModelManager().getFragmentsFor(
				model);
		for (int i = 0; i < fragments.length; i++) {
			IFragmentModel fmodel = fragments[i];
			IPath modelPath;
			if (relative) modelPath = ((ExternalPluginModelBase)fmodel).getEclipseHomeRelativePath();
			else modelPath = new Path(fmodel.getInstallLocation());
			IPath libraryPath = modelPath.append(name);
			IPath[] sourceAnnot =
				getSourceAnnotation(fmodel.getFragment(), modelPath, name, relative);
			IClasspathEntry libraryEntry =
				relative ? 
				JavaCore.newVariableEntry(
					libraryPath,
					sourceAnnot[0],
					sourceAnnot[1],
					exported):
				JavaCore.newLibraryEntry(
					libraryPath,
					sourceAnnot[0],
					sourceAnnot[1]);
			IClasspathEntry resolved = relative?
				JavaCore.getResolvedClasspathEntry(libraryEntry):libraryEntry;
			if (resolved != null && resolved.getPath().toFile().exists()) {
				// looks good - return it
				return libraryEntry;
			}
		}
		return null;
	}

	/*public static IPath getExternalPath(IPluginModelBase model) {
		IPath modelPath = new Path(PDECore.ECLIPSE_HOME_VARIABLE);
		modelPath =
			modelPath.append(
				((ExternalPluginModelBase) model).getEclipseHomeRelativePath());
		return modelPath;
	}*/

	public static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		IPath rootPath,
		boolean unconditionallyExport) {
		String name = expandLibraryName(library.getName());
		boolean variable = rootPath.segment(0).startsWith(PDECore.ECLIPSE_HOME_VARIABLE);
		IPath libraryPath = rootPath.append(name);
		IPath[] sourceAnnot =
			getSourceAnnotation(library.getPluginBase(), rootPath, name, variable);
		if (variable)
			return JavaCore.newVariableEntry(
				libraryPath,
				sourceAnnot[0],
				sourceAnnot[1],
				unconditionallyExport ? true : library.isFullyExported());
		else
			return JavaCore.newLibraryEntry(
				libraryPath,
				sourceAnnot[0],
				sourceAnnot[1],
				unconditionallyExport ? true : library.isFullyExported());
	}

	public static IClasspathEntry createLibraryEntryFromFragment(
		IFragmentModel fragment,
		IPluginLibrary library,
		IPath rootPath,
		boolean unconditionallyExport) {
		String name = expandLibraryName(library.getName());
		boolean variable = rootPath.segment(0).startsWith(PDECore.ECLIPSE_HOME_VARIABLE);
		IPath libraryPath = rootPath.append(name);
		IPath[] sourceAnnot =
			getSourceAnnotation(fragment.getPluginBase(), rootPath, name, true);
			
		// to accomodate cases where the fragment does not contain any library
		// entries, yet contains .jar files referenced by the parent plug-in.
		// e.g. the SWT case.	
		if (fragment instanceof WorkspaceFragmentModel
			&& fragment.getPluginBase().getLibraries().length == 0) {
			for (int i = 0; i < sourceAnnot.length; i++) {
				if (sourceAnnot[i] != null) {
					IPath resolvedPath = JavaCore.getResolvedVariablePath(sourceAnnot[i]);
					if (resolvedPath != null)
						sourceAnnot[i] = resolvedPath;
				}
			}
		}
		
		if (variable)
			return JavaCore.newVariableEntry(
				libraryPath,
				sourceAnnot[0],
				sourceAnnot[1],
				unconditionallyExport ? true : library.isFullyExported());
				
		return JavaCore.newLibraryEntry(
				libraryPath,
				sourceAnnot[0],
				sourceAnnot[1],
				unconditionallyExport ? true : library.isFullyExported());
	}
	
	private static boolean isEntryAdded(IPath path, int kind, Vector entries) {
		for (int i = 0; i < entries.size(); i++) {
			IClasspathEntry entry = (IClasspathEntry) entries.elementAt(i);
			if (entry.getEntryKind() == kind) {
				if (entry.getPath().equals(path))
					return true;
			}
		}
		return false;
	}

	public void addClasspathEntries(Vector result) {
		for (Iterator iter = checkedPlugins; iter.hasNext();) {
			PluginEntry element = (PluginEntry) iter.next();
			addToClasspathEntries(element, relative, true, result);
		}
	}

	public IClasspathEntry[] getClasspathEntries() {
		Vector result = new Vector();
		addClasspathEntries(result);
		IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
		result.copyInto(finalEntries);
		return finalEntries;
	}
	public static IPath getJREPath() {
		return JavaCore.getClasspathVariable(JRE_VAR);
	}
	public static IPath[] getJRESourceAnnotation() {
		IPath source = JavaCore.getClasspathVariable(JRE_SRCVAR);
		IPath prefix = JavaCore.getClasspathVariable(JRE_SRCROOTVAR);
		return new IPath[] { source, prefix };
	}

	private static IPath[] getSourceAnnotation(
		IPluginBase pluginBase,
		IPath rootPath,
		String name,
		boolean relative) {
		IPath[] annot = new IPath[2];
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			String zipName = name.substring(0, dot) + "src.zip";
			// test the sibling location
			if (exists(pluginBase, rootPath, zipName)) {
				annot[0] = rootPath.append(zipName);
			} else {
				// must look up source locations
				annot[0] = findSourceZip(pluginBase, zipName, relative);
			}
		}
		return annot;
	}

	private static IPath findSourceZip(
		IPluginBase pluginBase,
		String zipName,
		boolean relative) {
		SourceLocationManager manager =
			PDECore.getDefault().getSourceLocationManager();
		IPath sourcePath = manager.findVariableRelativePath(pluginBase, new Path(zipName));
		if (sourcePath!=null && !relative) {
			// expand the first segment as a variable
			String var = sourcePath.segment(0);
			IPath varPath = JavaCore.getClasspathVariable(var);
			if (varPath!=null) {
				sourcePath = varPath.append(sourcePath.removeFirstSegments(1));
			}
		}
		return sourcePath;
	}

	private static boolean exists(
		IPluginBase plugin,
		IPath rootPath,
		String zipName) {
		IResource resource = plugin.getModel().getUnderlyingResource();
		if (resource != null) {
			IProject project = resource.getProject();
			IFile file = project.getFile(zipName);
			return file.exists();
		} else {
			rootPath = getAbsolutePath(rootPath);
			File file = rootPath.append(zipName).toFile();
			return file.exists();
		}
	}

	private static IPath getAbsolutePath(IPath path) {
		String firstSegment = path.segment(0);
		if (firstSegment.startsWith(PDECore.ECLIPSE_HOME_VARIABLE)) {
			IPath root = JavaCore.getClasspathVariable(firstSegment);
			if (root != null)
				return root.append(path.removeFirstSegments(1));
		}
		return path;
	}

	public IClasspathEntry[] getSourceClasspathEntries(IPluginModel model) {
		Vector result = new Vector();

		addFoldersToClasspathEntries(model, result);
		IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
		result.copyInto(finalEntries);
		return finalEntries;
	}

	public static boolean isAlreadyPresent(
		IClasspathEntry[] oldEntries,
		IClasspathEntry entry) {
		for (int i = 0; i < oldEntries.length; i++) {
			IClasspathEntry oldEntry = oldEntries[i];
			if (oldEntry.getContentKind() == entry.getContentKind()
				&& oldEntry.getEntryKind() == entry.getEntryKind()
				&& oldEntry.getPath().equals(entry.getPath())) {
				return true;
			}
		}
		return false;
	}

	public static void addImplicitLibraries(
		Vector result,
		boolean relative,
		boolean addRuntime) {
		String bootId = "org.eclipse.core.boot";
		String runtimeId = "org.eclipse.core.runtime";
		IPlugin bootPlugin = PDECore.getDefault().findPlugin(bootId);

		if (addRuntime) {
			IPlugin runtimePlugin = PDECore.getDefault().findPlugin(runtimeId);
			if (runtimePlugin != null) {
				addToClasspathEntries(
					new PluginEntry(runtimePlugin),
					relative,
					true,
					result);
			}
		}
		if (bootPlugin != null) {
			addToClasspathEntries(new PluginEntry(bootPlugin), relative, true, result);
		}
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return "";
		if (source.charAt(0) != '$')
			return source;
		IPath path = new Path(source);
		String firstSegment = path.segment(0);
		if (firstSegment.charAt(firstSegment.length() - 1) != '$')
			return source;
		String variable = firstSegment.substring(1, firstSegment.length() - 1);
		variable = variable.toLowerCase();
		if (variable.equals("ws")) {
			variable = TargetPlatform.getWS();
			if (variable != null)
				variable = "ws" + File.separator + variable;
		} else if (variable.equals("os")) {
			variable = TargetPlatform.getOS();
			if (variable != null)
				variable = "os" + File.separator + variable;
		} else
			variable = null;
		if (variable != null) {
			path = path.removeFirstSegments(1);
			return variable + IPath.SEPARATOR + path.toString();
		}
		return source;
	}

}