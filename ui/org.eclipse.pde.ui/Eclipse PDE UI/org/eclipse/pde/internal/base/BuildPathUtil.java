package org.eclipse.pde.internal.base;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.preferences.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.model.build.*;
/**
 * A utility class that can be used by plug-in project
 * wizards to set up the Java build path. The actual
 * entries of the build path cannot be known to the
 * master wizard. The client wizards need to
 * add these entries depending on the code they
 * generate and the plug-ins they need to reference.
 * This class is typically used from within
 * a plug-in content wizard.
 */
public class BuildPathUtil {
/**
 * BuildPathUtil constructor comment.
 */
public BuildPathUtil() {
	super();
}

private static void ensureFolderExists(IProject project, IPath folderPath) throws CoreException {
	IWorkspace workspace = project.getWorkspace();
	if (!workspace.getRoot().exists(folderPath)) {
		IFolder folder = workspace.getRoot().getFolder(folderPath);
		folder.create(true, true, null);
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
	for (int i=0; i<libraries.length; i++) {
	    result.add(libraries[i]);
	}
	// add implicit libraries
	PluginPathUpdater.addImplicitLibraries(result);
	// JRE the last
	addJRE(result);
	IClasspathEntry [] entries = new IClasspathEntry [ result.size() ];
	result.copyInto(entries);
	javaProject.setRawClasspath(entries, monitor);
}
/**
 * Sets the Java build path of the provided plug-in model.
 * The model is expected to come from the workspace
 * and should have an underlying resource.
 * <p>This method will
 * @param model the plug-in project handle
 */

public static void setBuildPath(IPluginModel model, IProgressMonitor monitor)
	throws JavaModelException, CoreException {

	IProject project = model.getUnderlyingResource().getProject();		
	IJavaProject javaProject = JavaCore.create(project);
	// Set classpath
	Vector result = new Vector();
	addSourceFolders(model.getBuildModel(), result);
	addDependencies(project, model.getPlugin().getImports(), result);
	// add implicit libraries
	PluginPathUpdater.addImplicitLibraries(result);
	addJRE(result);
	IClasspathEntry [] entries = new IClasspathEntry [ result.size() ];
	result.copyInto(entries);
	javaProject.setRawClasspath(entries, monitor);
}

private static void addSourceFolders(IBuildModel model, Vector result)throws CoreException {
	IBuild build = model.getBuild();
	IBuildEntry [] entries = build.getBuildEntries();
	for (int i=0; i<entries.length; i++) {
		IBuildEntry entry = entries[i];
		if (entry.getName().startsWith("source.")) {
			String [] folders = entry.getTokens();
			for (int j=0; j<folders.length; j++) {
				addSourceFolder(folders[j], 
				       model.getUnderlyingResource().getProject(), 
				       result);
			}
		}
	}
}

private static void addSourceFolder(String name, IProject project, Vector result) throws CoreException {
	IPath path = project.getFullPath().append(name);
	ensureFolderExists(project, path);
	IClasspathEntry entry = JavaCore.newSourceEntry(path);
	result.add(entry);
}

private static void addDependencies(IProject project, IPluginImport[] imports, Vector result) {
	Vector checkedPlugins = new Vector();
	for (int i=0; i<imports.length; i++) {
		IPluginImport iimport = imports[i];
		String id = iimport.getId();
		IPlugin ref = PDEPlugin.getDefault().findPlugin(id);
		if (ref!=null) {
			checkedPlugins.add(new PluginPathUpdater.CheckedPlugin(ref, true));
		}
	}
	PluginPathUpdater ppu = new PluginPathUpdater(project, checkedPlugins.iterator());
	ppu.addClasspathEntries(result);
}

private static void addJRE(Vector result) {
	IPath jrePath = new Path("JRE_LIB");
	IPath[] annot= new IPath[2];
	annot[0] = new Path("JRE_SRC");
	annot[1] = new Path("JRE_SRCROOT");
	if (jrePath!=null)
	   result.add(JavaCore.newVariableEntry(jrePath, annot[0], annot[1]));
}

}
