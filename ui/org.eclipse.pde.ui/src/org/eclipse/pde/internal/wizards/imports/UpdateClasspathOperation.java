/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.LibraryModel;
import org.eclipse.core.runtime.model.PluginFragmentModel;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

public class UpdateClasspathOperation implements IWorkspaceRunnable {

	private IJavaProject fJavaProject;
	private PluginModel fPlugin;
	private IClasspathEntry[] fLibraryClasspathEntries;
	private IPath fOutputLocation;
	private IWorkspaceRoot fRoot;

	public UpdateClasspathOperation(IJavaProject jproject, PluginModel plugin, IClasspathEntry[] libraryClasspathEntries, IPath outputLocation) {
		fJavaProject= jproject;
		fPlugin= plugin;
		fOutputLocation= outputLocation;
		fLibraryClasspathEntries= libraryClasspathEntries;
		fRoot= ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask("Setting class path of '" + fJavaProject.getElementName() + "'...", 1);
		try {
			IProject proj= fJavaProject.getProject();
			
			// add library entries
			ArrayList entries= new ArrayList();
			
			for (int i= 0; i < fLibraryClasspathEntries.length; i++) {
				entries.add(fLibraryClasspathEntries[i]);
			}
			
			// add project prerequisits
			if (entries.size() > 0) {
				addProjectClasspathEntries(proj, fPlugin, entries);
			}
			
			// add JRE
			entries.add(JavaRuntime.getJREVariableEntry());

			IClasspathEntry[] newClasspath= (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
			
			IStatus validation= JavaConventions.validateClasspath(fJavaProject, newClasspath, fOutputLocation);
			if (!validation.isOK()) {
				throw new CoreException(validation);
			}
			
			fJavaProject.setRawClasspath(newClasspath, fOutputLocation, new SubProgressMonitor(monitor, 1));
		
		} finally {
			monitor.done();
		}		
		
	}
	
	private void addProjectClasspathEntries(IProject project, PluginModel plugin, ArrayList entries) {
		// avoid duplicate project entries
		HashSet projectsAdded= new HashSet();
				
		// boot project & runtime project are implicitly imported
		String prjName= project.getName();
		if (!"org.eclipse.core.boot".equals(prjName)) {
			IProject bootProj= fRoot.getProject("org.eclipse.core.boot");
			entries.add(JavaCore.newProjectEntry(bootProj.getFullPath()));
			projectsAdded.add(bootProj);
			if (!"org.eclipse.core.runtime".equals(prjName) && !"org.apache.xerces".equals(prjName)) {
				IProject runtimeProj= fRoot.getProject("org.eclipse.core.runtime");
				entries.add(JavaCore.newProjectEntry(runtimeProj.getFullPath()));
				projectsAdded.add(runtimeProj);
			}
		}
		
		// for fragments add the parent plugin as prerequisit
		if (plugin instanceof PluginFragmentModel) {
			String parentPluginName= ((PluginFragmentModel)plugin).getPlugin();
			IProject parentProject= fRoot.getProject(parentPluginName);
			entries.add(JavaCore.newProjectEntry(parentProject.getFullPath()));
			projectsAdded.add(parentProject);
		}
		
		PluginPrerequisiteModel[] required= plugin.getRequires();
		// all required projects
		if (required != null) {
			for (int i= 0; i < required.length; i++) {
				PluginPrerequisiteModel curr= required[i];
				IProject req= fRoot.getProject(curr.getPlugin());
				if (!projectsAdded.contains(req)) {
					IClasspathEntry entry= JavaCore.newProjectEntry(req.getFullPath(), curr.getExport());
					entries.add(entry);
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

	private static IPath getLibraryPath(IProject project, LibraryModel curr) {
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
	
	public static IClasspathEntry getLibraryEntry(IProject project, LibraryModel libraryModel, boolean exported) {
		IPath jarPath= getLibraryPath(project, libraryModel);
		IPath srcAttach= getSourceAttchmentPath(project, jarPath);
		IPath srcRoot= srcAttach != null ? Path.EMPTY : null;
		return JavaCore.newLibraryEntry(jarPath, srcAttach, srcRoot, exported);
	}	
			
}

