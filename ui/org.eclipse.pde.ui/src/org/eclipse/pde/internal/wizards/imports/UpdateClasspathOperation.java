/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.plugin.*;

import org.eclipse.jdt.core.*;

import org.eclipse.jdt.launching.JavaRuntime;

public class UpdateClasspathOperation implements IWorkspaceRunnable {

	private IJavaProject javaProject;
	private IPluginModelBase model;
	private IClasspathEntry[] libraryClasspathEntries;
	private IPath outputLocation;
	private IWorkspaceRoot root;

	public UpdateClasspathOperation(IJavaProject jproject, IPluginModelBase model, IClasspathEntry[] libraryClasspathEntries, IPath outputLocation) {
		this.javaProject= jproject;
		this.model = model;
		this.outputLocation= outputLocation;
		this.libraryClasspathEntries= libraryClasspathEntries;
		root= ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask("Setting class path of '" + javaProject.getElementName() + "'...", 1);
		try {
			IProject proj= javaProject.getProject();
			
			// add library entries
			ArrayList entries= new ArrayList();
			IWorkspaceRoot root = proj.getWorkspace().getRoot();
			
			for (int i= 0; i < libraryClasspathEntries.length; i++) {
				IClasspathEntry entry = libraryClasspathEntries[i];
				if (root.findMember(entry.getPath())!=null)
				   entries.add(libraryClasspathEntries[i]);
			}
			
			// add project prerequisits
			if (entries.size() > 0) {
				addProjectClasspathEntries(proj, model, entries);
			}
			
			// add JRE
			entries.add(JavaRuntime.getJREVariableEntry());

			IClasspathEntry[] newClasspath= (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
			
			IStatus validation= JavaConventions.validateClasspath(javaProject, newClasspath, outputLocation);
			if (!validation.isOK()) {
				throw new CoreException(validation);
			}
			
			javaProject.setRawClasspath(newClasspath, outputLocation, new SubProgressMonitor(monitor, 1));
		
		} finally {
			monitor.done();
		}		
		
	}
	
	private void addProjectClasspathEntries(IProject project, IPluginModelBase model, ArrayList entries) {
		// avoid duplicate project entries
		HashSet projectsAdded= new HashSet();
				
		// boot project & runtime project are implicitly imported
		String prjName= project.getName();
		if (!"org.eclipse.core.boot".equals(prjName)) {
			IProject bootProj= root.getProject("org.eclipse.core.boot");
			entries.add(JavaCore.newProjectEntry(bootProj.getFullPath()));
			projectsAdded.add(bootProj);
			if (!"org.eclipse.core.runtime".equals(prjName) && !"org.apache.xerces".equals(prjName)) {
				IProject runtimeProj= root.getProject("org.eclipse.core.runtime");
				entries.add(JavaCore.newProjectEntry(runtimeProj.getFullPath()));
				projectsAdded.add(runtimeProj);
			}
		}
		
		// for fragments add the parent plugin as prerequisit
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel)model).getFragment();
			String parentPluginId= fragment.getPluginId();
			IProject parentProject= root.getProject(parentPluginId);
			entries.add(JavaCore.newProjectEntry(parentProject.getFullPath()));
			projectsAdded.add(parentProject);
		}
		else {
			IPlugin plugin = ((IPluginModel)model).getPlugin();
			IPluginImport[] imports= plugin.getImports();
			// all required projects
			if (imports.length>0) {
				for (int i= 0; i < imports.length; i++) {
					IPluginImport curr= imports[i];
					IProject req= root.getProject(curr.getId());
					if (!projectsAdded.contains(req)) {
						IClasspathEntry entry= JavaCore.newProjectEntry(req.getFullPath(), curr.isReexported());
						entries.add(entry);
					}
				}
			}
		}
	}	
	
	
	private static IPath getSourceAttchmentPath(IProject project, IPath jarPath) {
		String libName= jarPath.lastSegment();
		int idx= libName.lastIndexOf('.');
		if (idx != -1) {
			String srcName= libName.substring(0, idx) + "src.zip";
			IPath path= jarPath.removeLastSegments(1).append(srcName);
			IWorkspaceRoot root= project.getWorkspace().getRoot();
			if (root.findMember(path) != null) {
				return path;
			}
		}
		return null;
	}

	private static IPath getLibraryPath(IProject project, IPluginLibrary curr) {
		IPath path= new Path(curr.getName());
		String first= path.segment(0);
		if (first != null) {
			IPath rest= path.removeFirstSegments(1);
			if (first.equals("$ws$")) {
				path= new Path("ws").append(BootLoader.getWS()).append(rest);
			} else if (first.equals("$os$")) {
				path= new Path("os").append(BootLoader.getOS()).append(rest);
			} else if (first.equals("$nl$")) {
				path= new Path("nl").append(BootLoader.getNL()).append(rest);
			}
		}
		return project.getFullPath().append(path);
	}
	
	public static IClasspathEntry getLibraryEntry(IProject project, IPluginLibrary library, boolean exported) {
		IPath jarPath= getLibraryPath(project, library);
		IPath srcAttach= getSourceAttchmentPath(project, jarPath);
		IPath srcRoot= srcAttach != null ? Path.EMPTY : null;
		return JavaCore.newLibraryEntry(jarPath, srcAttach, srcRoot, exported);
	}	
			
}

