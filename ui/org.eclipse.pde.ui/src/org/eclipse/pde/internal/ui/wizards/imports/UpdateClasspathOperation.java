/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.pde.internal.ui.TargetPlatform;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

import org.eclipse.jdt.core.*;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.PDECore;

public class UpdateClasspathOperation implements IWorkspaceRunnable {

	private IJavaProject javaProject;
	private IPluginModelBase model;
	private IPluginModelBase[] models;
	private IFragmentModel[] fragments;
	private IClasspathEntry[] libraryClasspathEntries;
	private IPath outputLocation;
	private IWorkspaceRoot root;

	public UpdateClasspathOperation(
		IJavaProject jproject,
		IPluginModelBase model,
		IPluginModelBase[] models,
		IClasspathEntry[] libraryClasspathEntries,
		IPath outputLocation) {
		this.javaProject = jproject;
		this.model = model;
		this.models = models;
		this.outputLocation = outputLocation;
		this.libraryClasspathEntries = libraryClasspathEntries;
		root = ResourcesPlugin.getWorkspace().getRoot();
		createFragments();
	}

	private void createFragments() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (models[i].isFragmentModel())
				result.add(models[i]);
		}
		fragments =
			(IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask("", 1);
		try {
			IProject proj = javaProject.getProject();

			// add library entries
			ArrayList entries = new ArrayList();
			IWorkspaceRoot root = proj.getWorkspace().getRoot();

			for (int i = 0; i < libraryClasspathEntries.length; i++) {
				IClasspathEntry entry = libraryClasspathEntries[i];
				if (entries.contains(entry)) continue;
				if (root.findMember(entry.getPath()) != null)
					entries.add(entry);
				else if (model.isFragmentModel() == false) {
					resolveEntryInFragments(root, entry, entries);
				}
			}

			// add project prerequisits
			//if (entries.size() > 0) {
			addProjectClasspathEntries(proj, entries);
			//}

			// add JRE
			entries.add(JavaRuntime.getJREVariableEntry());

			IClasspathEntry[] newClasspath =
				(IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);

			IStatus validation =
				JavaConventions.validateClasspath(javaProject, newClasspath, outputLocation);
			if (!validation.isOK()) {
				System.out.println("Invalid classpath for " + proj.getName() + ": " + entries);
				throw new CoreException(validation);
			}

			javaProject.setRawClasspath(
				newClasspath,
				outputLocation,
				new SubProgressMonitor(monitor, 1));

		} finally {
			monitor.done();
		}
	}

	private void resolveEntryInFragments(
		IWorkspaceRoot root,
		IClasspathEntry entry,
		ArrayList entries) {
		IProject[] projects = root.getProjects();

		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.exists() && project.isOpen()) {
				if (resolveEntry(root, project, entry, entries))
					break;
			}
		}
	}
	private boolean resolveEntry(
		IWorkspaceRoot root,
		IProject project,
		IClasspathEntry entry,
		ArrayList entries) {
		IPlugin plugin = (IPlugin) model.getPluginBase();
		for (int i = 0; i < fragments.length; i++) {
			IFragmentModel fmodel = fragments[i];
			IFragment fragment = fmodel.getFragment();
			if (fragment.getId().equals(project.getName())) {
				String fid = fragment.getPluginId();
				String fversion = fragment.getPluginVersion();
				if (PDECore
					.compare(
						fid,
						fversion,
						plugin.getId(),
						plugin.getVersion(),
						fragment.getRule())) {
					// Match - try this one
					IPath path = entry.getPath().removeFirstSegments(1);
					String name = path.toString();
					IClasspathEntry newEntry = getLibraryEntry(project, name, true);
					if (root.exists(newEntry.getPath())) {
						// Resolved - finish
						if (!entries.contains(entry))
							entries.add(newEntry);
						return true;
					}
				}
			}
		}
		return false;
	}

	private void addProjectClasspathEntries(IProject project, ArrayList entries) {
		// avoid duplicate project entries
		HashSet projectsAdded = new HashSet();

		// for fragments add the parent plugin as prerequisit
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			String parentPluginId = fragment.getPluginId();
			IProject parentProject = root.getProject(parentPluginId);
			entries.add(JavaCore.newProjectEntry(parentProject.getFullPath()));
			projectsAdded.add(parentProject);
		} else {
			// add fragment libraries
			IPlugin plugin = ((IPluginModel) model).getPlugin();
			//addFragmentLibraries(plugin, entries);
		}
		// add the prerequisites
		IPluginBase plugin = ((IPluginModelBase) model).getPluginBase();
		IPluginImport[] imports = plugin.getImports();
		// all required projects
		if (imports.length > 0) {
			for (int i = 0; i < imports.length; i++) {
				IPluginImport curr = imports[i];
				IProject req = root.getProject(curr.getId());
				if (!projectsAdded.contains(req)) {
					IClasspathEntry entry =
						JavaCore.newProjectEntry(req.getFullPath(), curr.isReexported());
					entries.add(entry);
					projectsAdded.add(req);
				}
			}
		}
		// boot project & runtime project are implicitly imported
		String prjName = project.getName();
		if (!"org.eclipse.core.boot".equals(prjName)) {
			IProject bootProj = root.getProject("org.eclipse.core.boot");
			if (!projectsAdded.contains(bootProj)) {
				entries.add(JavaCore.newProjectEntry(bootProj.getFullPath()));
				projectsAdded.add(bootProj);
			}
			if (!"org.eclipse.core.runtime".equals(prjName)
				&& !"org.apache.xerces".equals(prjName)) {
				IProject runtimeProj = root.getProject("org.eclipse.core.runtime");
				if (!projectsAdded.contains(runtimeProj)) {
					entries.add(JavaCore.newProjectEntry(runtimeProj.getFullPath()));
					projectsAdded.add(runtimeProj);
				}
			}
		}
	}

	private void addFragmentLibraries(IPlugin plugin, ArrayList entries) {
		for (int i = 0; i < fragments.length; i++) {
			IFragmentModel fmodel = fragments[i];
			IFragment fragment = fmodel.getFragment();
			if (PDECore
				.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					plugin.getId(),
					plugin.getVersion(),
					fragment.getRule())) {
				IPluginLibrary[] libraries = fragment.getLibraries();
				for (int j = 0; j < libraries.length; j++) {
					IProject project = root.getProject(fragment.getId());
					IClasspathEntry entry = getLibraryEntry(project, libraries[j], true);
					if (root.exists(entry.getPath())) {
						if (!entries.contains(entry))
							entries.add(entry);
					}
				}
			}
		}
	}

	private static IPath getSourceAttachmentPath(IProject project, IPath jarPath) {
		IPath sourcePath = getSourcePath(jarPath);
		if (sourcePath==null) return null;
		IWorkspaceRoot root = project.getWorkspace().getRoot();
		if (root.findMember(sourcePath) != null) {
			return sourcePath;
		}
		return null;
	}
	
	static IPath getExpandedPath(IPath path) {
		String first = path.segment(0);
		if (first != null) {
			IPath rest = path.removeFirstSegments(1);
			if (first.equals("$ws$")) {
				path = new Path("ws").append(TargetPlatform.getWS()).append(rest);
			} else if (first.equals("$os$")) {
				path = new Path("os").append(TargetPlatform.getOS()).append(rest);
			} else if (first.equals("$nl$")) {
				path = new Path("nl").append(TargetPlatform.getNL()).append(rest);
			} else if (first.equals("$arch$")) {
				path = new Path("arch").append(TargetPlatform.getOSArch()).append(rest);
			}
		}
		return path;
	}
	
	static IPath getSourcePath(IPath jarPath) {
		jarPath = getExpandedPath(jarPath);
		String libName = jarPath.lastSegment();
		int idx = libName.lastIndexOf('.');
		if (idx != -1) {
			String srcName = libName.substring(0, idx) + "src.zip";
			IPath path = jarPath.removeLastSegments(1).append(srcName);
			return path;
		}
		else return null;
	}

	private static IPath getLibraryPath(IProject project, IPluginLibrary curr) {
		return getLibraryPath(project, curr.getName());
	}

	private static IPath getLibraryPath(IProject project, String libraryName) {
		IPath path = new Path(libraryName);
		path = getExpandedPath(path);
		return project.getFullPath().append(path);
	}

	public static IClasspathEntry getLibraryEntry(
		IProject project,
		IPluginLibrary library,
		boolean exported) {
		return getLibraryEntry(project, library.getName(), exported);
	}

	private static IClasspathEntry getLibraryEntry(
		IProject project,
		String libraryName,
		boolean exported) {
		IPath jarPath = getLibraryPath(project, libraryName);
		IPath srcAttach = getSourceAttachmentPath(project, jarPath);
		IPath srcRoot = srcAttach != null ? Path.EMPTY : null;
		return JavaCore.newLibraryEntry(jarPath, srcAttach, srcRoot, exported);
	}

}