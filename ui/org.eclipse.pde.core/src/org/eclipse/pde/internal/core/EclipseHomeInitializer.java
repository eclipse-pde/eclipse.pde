/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Created on Apr 10, 2002
 *
 * To change this generated comment edit the template variable "filecomment":
 * Workbench>Preferences>Java>Templates.
 */
package org.eclipse.pde.internal.core;


import java.io.File;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 *
 */
public class EclipseHomeInitializer extends ClasspathVariableInitializer {

	static class SetEclipseVariablesOperation implements IWorkspaceRunnable {
		public void run(IProgressMonitor monitor) throws CoreException {
			String[] variables = JavaCore.getClasspathVariableNames();
			for (int i = 0; i < variables.length; i++) {
				if (variables[i].startsWith(PDECore.ECLIPSE_HOME_VARIABLE)
					&& !variables[i].equals(PDECore.ECLIPSE_HOME_VARIABLE)) {
					JavaCore.removeClasspathVariable(variables[i], null);
				}
			}
			try {
				Preferences pref = PDECore.getDefault().getPluginPreferences();
				String platformHome = pref.getString(ICoreConstants.PLATFORM_PATH);
				JavaCore.setClasspathVariable(
					PDECore.ECLIPSE_HOME_VARIABLE,
					new Path(platformHome),
					null);

				File[] linkFiles = PluginPathFinder.getLinkFiles(platformHome);
				if (linkFiles != null) {
					for (int i = 0; i < linkFiles.length; i++) {
						String path =
							PluginPathFinder.getPath(platformHome, linkFiles[i]);
						if (path != null) {
							String variable =
								PDECore.ECLIPSE_HOME_VARIABLE
									+ "_"
									+ linkFiles[i]
										.getName()
										.replace('.', '_')
										.toUpperCase();
							JavaCore.setClasspathVariable(variable, new Path(path), null);
						}
					}
				}
			} catch (JavaModelException e) {
			}

		}

	}
	
	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		resetEclipseHomeVariables();
	}

	public static void resetEclipseHomeVariables() {
		try {
			JavaCore.run(new EclipseHomeInitializer.SetEclipseVariablesOperation(), null);
		} catch (CoreException e) {
		}
	}

	public static IPath createEclipseRelativeHome(String installLocation) {
		IPath fullPath = new Path(installLocation);

		String[] variables = JavaCore.getClasspathVariableNames();

		String correctVariable = null;
		int maxMatching = 0;

		for (int i = 0; i < variables.length; i++) {
			if (variables[i].startsWith(PDECore.ECLIPSE_HOME_VARIABLE)) {
				IPath currentPath = JavaCore.getClasspathVariable(variables[i]);
				int currentMatch = fullPath.matchingFirstSegments(currentPath);
				if (currentMatch > maxMatching) {
					maxMatching = currentMatch;
					correctVariable = variables[i];
				}
			}
		}
		return (correctVariable == null)
			? fullPath
			: new Path(correctVariable).append(fullPath.removeFirstSegments(maxMatching));
	}

}
