package org.eclipse.pde.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.*;
import java.util.Vector;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.PluginPathUpdater;
/**
 * A utility class that can be used by plug-in project
 * wizards to set up the Java build path. The actual
 * entries of the build path are not known in the
 * master wizard. The client wizards need to
 * add these entries depending on the code they
 * generate and the plug-ins they need to reference.
 * This class is typically used from within
 * a plug-in content wizard.
 * <p>
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public class BuildPathUtil {
	/**
	 * The default constructor.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public BuildPathUtil() {
		super();
	}

	private static void ensureFolderExists(IProject project, IPath folderPath)
		throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i=1;i<=folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
		}
	}
	/**
	 * Sets the Java build path of the project
	 * using plug-in structure data and
	 * provided entries. These entries are
	 * created in plug-in content wizards
	 * based on the plug-ins required by
	 * the generated code.
	 * @param project the plug-in project handle
	 * @param data structure data passed in by the master wizard
	 * @param libraries an array of the library entries to be set
	 * @param monitor for reporting progress
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static void setBuildPath(
		IProject project,
		IPluginStructureData data,
		IClasspathEntry[] libraries,
		IProgressMonitor monitor)
		throws JavaModelException, CoreException {

		// Set output folder
		IJavaProject javaProject = JavaCore.create(project);
		IPath path = project.getFullPath().append(data.getJavaBuildFolderName());
		javaProject.setOutputLocation(path, monitor);

		// Set classpath
		Vector result = new Vector();
		// Source folder first
		addSourceFolder(data.getSourceFolderName(), project, result);
		// Then the libraries
		for (int i = 0; i < libraries.length; i++) {
			result.add(libraries[i]);
		}
		// add implicit libraries
		addImplicitLibraries(result, data.getPluginId());
		// JRE the last
		addJRE(result);
		IClasspathEntry[] entries = new IClasspathEntry[result.size()];
		result.copyInto(entries);
		javaProject.setRawClasspath(entries, monitor);
	}

	/**
	 * Sets the Java build path of the provided plug-in model.
	 * The model is expected to come from the workspace
	 * and should have an underlying resource.
	 * 
	 * @param model the plug-in project handle
	 * @param monitor for reporting progress
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */

	public static void setBuildPath(
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws JavaModelException, CoreException {

		IProject project = model.getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
		// Set classpath
		Vector result = new Vector();
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IFile buildFile = project.getFile("build.properties");
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		if (buildModel != null)
			addSourceFolders(buildModel, result);
		else {
			// just keep the source folders
			keepExistingSourceFolders(javaProject, result);
		}

		// add own libraries, if present
		addLibraries(model, false, result);

		// add dependencies
		addDependencies(project, model.getPluginBase().getImports(), result);

		// if fragment, add referenced plug-in
		if (model instanceof IFragmentModel) {
			addFragmentPlugin((IFragmentModel) model, result);
			IPlugin parentPlugin = findFragmentPlugin((IFragmentModel)model);
			if (parentPlugin!=null) {
				addDependencies(project, parentPlugin.getImports(), result);
			}
		} else {
			addFragmentLibraries((IPluginModel) model, result, monitor);
		}
		// add implicit libraries
		addImplicitLibraries(result, model.getPluginBase().getId());
		addJRE(result);
		IClasspathEntry[] entries = new IClasspathEntry[result.size()];
		result.copyInto(entries);

		IStatus validation =
				JavaConventions.validateClasspath(javaProject, entries, javaProject.getOutputLocation());
		if (!validation.isOK()) {
			throw new CoreException(validation);
		}

		javaProject.setRawClasspath(entries, monitor);
	}

	private static void addImplicitLibraries(Vector result, String id) {
		boolean addRuntime = true;
		if (id.equals("org.eclipse.core.boot"))
			return;
		if (id.equals("org.eclipse.core.runtime") || id.equals("org.apache.xerces"))
			addRuntime = false;
		PluginPathUpdater.addImplicitLibraries(result, addRuntime);
	}

	private static void addSourceFolders(IBuildModel model, Vector result)
		throws CoreException {
		IBuild build = model.getBuild();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			IBuildEntry entry = entries[i];
			if (entry.getName().startsWith("source.")) {
				String[] folders = entry.getTokens();
				for (int j = 0; j < folders.length; j++) {
					addSourceFolder(folders[j], model.getUnderlyingResource().getProject(), result);
				}
			}
		}
	}

	private static void keepExistingSourceFolders(
		IJavaProject jproject,
		Vector result)
		throws CoreException {
		IClasspathEntry[] entries = jproject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
				&& entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
				result.add(entry);
			}
		}
	}

	private static void addSourceFolder(
		String name,
		IProject project,
		Vector result)
		throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path);
		IClasspathEntry entry = JavaCore.newSourceEntry(path);
		result.add(entry);
	}

	private static void addLibraries(
		IPluginModelBase model,
		boolean unconditionallyExport,
		Vector result) {
		IPluginBase pluginBase = model.getPluginBase();
		IPluginLibrary[] libraries = pluginBase.getLibraries();
		IPath rootPath = getRootPath(model);

		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			IClasspathEntry entry =
				PluginPathUpdater.createLibraryEntry(library, rootPath, unconditionallyExport);
			if (exists(model, entry))
				result.add(entry);
			else {
				// missing entry - search fragments
				if (!model.isFragmentModel()) {
					resolveLibraryInFragments(model, library, result);
				}
			}
		}
	}

	private static void resolveLibraryInFragments(
		IPluginModelBase model,
		IPluginLibrary library,
		Vector result) {
		IPlugin plugin = (IPlugin) model.getPluginBase();
		IResource resource = model.getUnderlyingResource();

		IFragmentModel[] fmodels;

		if (resource != null)
			fmodels =
				PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
		else
			fmodels =
				PDECore.getDefault().getExternalModelManager().getFragmentModels(null);
		for (int i = 0; i < fmodels.length; i++) {
			IFragmentModel fmodel = fmodels[i];
			if (fmodel.isEnabled() == false)
				continue;

			IFragment fragment = fmodel.getFragment();
			if (PDECore
				.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					plugin.getId(),
					plugin.getVersion(),
					fragment.getRule())) {

				IClasspathEntry entry =
					PluginPathUpdater.createLibraryEntry(library, getRootPath(fmodel), false);
				if (exists(fmodel, entry)) {
					result.add(entry);
					// we resolved the missing library - no
					// need to search any more
					break;
				}
			}
		}
	}

	private static IPath getRootPath(IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		IProject project = resource != null ? resource.getProject() : null;

		if (project != null)
			return project.getFullPath();
		else
			return PluginPathUpdater.getExternalPath(model);
	}

	private static boolean exists(IPluginModelBase model, IClasspathEntry entry) {
		IResource resource = model.getUnderlyingResource();
		IProject project = resource != null ? resource.getProject() : null;
		IPath path = entry.getPath();
		if (project == null) {
			return path.isValidPath(path.toString());
		} else {
			IWorkspaceRoot root = project.getWorkspace().getRoot();
			return root.findMember(path) != null;
		}
	}

	private static void addDependencies(
		IProject project,
		IPluginImport[] imports,
		Vector result) {
		Vector checkedPlugins = new Vector();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			String id = iimport.getId();
			String version = iimport.getVersion();
			int match = iimport.getMatch();
			IPlugin ref = PDECore.getDefault().findPlugin(id, version, match);
			if (ref != null) {
				PluginPathUpdater.CheckedPlugin cplugin =
					new PluginPathUpdater.CheckedPlugin(ref, true);
				cplugin.setExported(iimport.isReexported());
				checkedPlugins.add(cplugin);
			}
		}
		PluginPathUpdater ppu =
			new PluginPathUpdater(project, checkedPlugins.iterator());
		ppu.addClasspathEntries(result);
	}
	
	private static IPlugin findFragmentPlugin(IFragmentModel model) {
		IFragment fragment = model.getFragment();
		String id = fragment.getPluginId();
		String version = fragment.getPluginVersion();
		int match = fragment.getRule();

		return PDECore.getDefault().findPlugin(id, version, match);
	}

	private static void addFragmentPlugin(
		IFragmentModel model,
		Vector result) {
		IPlugin plugin = findFragmentPlugin(model);
		if (plugin != null) {
			IProject project = null;
			if (plugin.getModel() instanceof WorkspacePluginModel) {
				project =
					plugin.getModel().getUnderlyingResource().getProject();
			}
			Vector checkedPlugins = new Vector();
			checkedPlugins.add(
				new PluginPathUpdater.CheckedPlugin(plugin, true));
			PluginPathUpdater ppu =
				new PluginPathUpdater(project, checkedPlugins.iterator());
			ppu.addClasspathEntries(result);
		}
	}

	private static void addFragmentLibraries(
		IPluginModel model,
		Vector result,
		IProgressMonitor monitor) {
		IPlugin plugin = model.getPlugin();
		ArrayList fragments = new ArrayList();
		createFragmentList(model, fragments, monitor);
		for (int i = 0; i < fragments.size(); i++) {
			IFragment fragment = (IFragment)fragments.get(i);
			addLibraries(fragment.getModel(), true, result);
		}
	}
	
	private static void createFragmentList(IPluginModel model, ArrayList fragments, IProgressMonitor monitor) {
		addFragments(model, PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels(),
					fragments);
		addFragments(model, PDECore.getDefault().getExternalModelManager().getFragmentModels(monitor),
					fragments);
		
	}

	private static void addFragments(
		IPluginModel pluginModel,
		IFragmentModel[] models,
		ArrayList result) {
		IPlugin plugin = pluginModel.getPlugin();
		for (int i = 0; i < models.length; i++) {
			if (models[i].isEnabled()==false) continue;
			IFragment fragment = models[i].getFragment();
			if (PDECore
				.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					plugin.getId(),
					plugin.getVersion(),
					fragment.getRule())) {
				addFragment(fragment, result);
			}
			
		}
	}
	private static void addFragment(IFragment fragment, ArrayList result) {
		for (int i=0; i<result.size(); i++) {
			IFragment curr = (IFragment)result.get(i);
			if (curr.getId().equals(fragment.getId())) return;
		}
		result.add(fragment);
	}

	private static void addJRE(Vector result) {
		IPath jrePath = new Path("JRE_LIB");
		IPath[] annot = new IPath[2];
		annot[0] = new Path("JRE_SRC");
		annot[1] = new Path("JRE_SRCROOT");
		if (jrePath != null)
			result.add(JavaCore.newVariableEntry(jrePath, annot[0], annot[1]));
	}
	
}