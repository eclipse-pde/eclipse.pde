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
	ensureFolderExists(project, path);
	javaProject.setOutputLocation(path, monitor);

	// Set classpath
	Vector result = new Vector();
	// Source folder first
	path = project.getFullPath().append(data.getSourceFolderName());
	ensureFolderExists(project, path);
	result.add(JavaCore.newSourceEntry(path));
	// Then the libraries
	for (int i=0; i<libraries.length; i++) {
	    result.add(libraries[i]);
	}
	// JDK the last
	IPath jdkPath = new Path("JRE_LIB");
	IPath[] annot= new IPath[2];
	annot[0] = new Path("JRE_SRC");
	annot[1] = new Path("JRE_SRCROOT");
	if (jdkPath!=null)
	   result.add(JavaCore.newVariableEntry(jdkPath, annot[0], annot[1]));
	IClasspathEntry [] entries = new IClasspathEntry [ result.size() ];
	result.copyInto(entries);
	javaProject.setRawClasspath(entries, monitor);
}
}
