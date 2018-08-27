/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.apiusescan.tests;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.osgi.service.prefs.BackingStoreException;

public class ExternalDependencyTestUtils {

	public static String PROJECT_NAME = "tests.apiusescan.coretestproject"; //$NON-NLS-1$
	public static String fReportLocation;
	private static IProject fTestProject;

	/**
	 * Unzips the test project. Opens it before returning it
	 *
	 * @return returns the <code>IProject</code>
	 */
	public static IProject setupProject() {
		IPath pluginDirectoryPath = TestSuiteHelper.getPluginDirectoryPath();
		String path = pluginDirectoryPath.append(new Path("/test-apiusescan/projects/" + PROJECT_NAME + ".zip")).toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
		File sourceFile = new File(path);
		if (!sourceFile.exists()) {
			return null;
		}

		enableExternalDependencyCheckOptions(true);

		fTestProject = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			if (fTestProject.exists()) {
				fTestProject.delete(true, new NullProgressMonitor());
			}
			Util.unzip(path, root.getLocation().toOSString());
			fTestProject.create(null);
			fTestProject.open(null);
			// fTestProject.refreshLocal(IResource.DEPTH_INFINITE, null);

		} catch (Exception e) {
			return null;
		}

		return fTestProject;
	}

	private static void enableExternalDependencyCheckOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY, value);
		inode.put(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY, value);
		inode.put(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * @return the test project
	 */
	public static IProject getProject() {
		if (fTestProject == null) {
			setupProject();
		}
		return fTestProject;
	}

	public static String setupReport(String reportName, boolean asDir) {
		fReportLocation = TestSuiteHelper.getPluginDirectoryPath() + "/test-apiusescan/reports/"; //$NON-NLS-1$
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String location = null;
		try {
			if (asDir) {
				String destLoc = root.getLocation().toOSString() + "/Reports/"; //$NON-NLS-1$
				Util.delete(new File(destLoc));
				try {
					Util.unzip(fReportLocation + reportName + ".zip", destLoc); //$NON-NLS-1$
				} catch (CoreException e) {
				}
				location = destLoc + reportName + File.separator + IApiCoreConstants.XML;
			} else {
				File newFile = new File(root.getLocation().toOSString() + "/Reports/" + reportName + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
				Util.delete(newFile);
				newFile.getParentFile().mkdirs();
				newFile.createNewFile();
				boolean result = Util.copy(new File(fReportLocation + reportName + ".zip"), newFile); //$NON-NLS-1$
				location = result ? newFile.getAbsolutePath() : null;
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		UseScanManager.getInstance().setReportLocations(new String[] { location });
		return location;
	}

	/**
	 * Wait for the running build to the complete
	 */
	public static void waitForBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				return;
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
		return;
	}
}