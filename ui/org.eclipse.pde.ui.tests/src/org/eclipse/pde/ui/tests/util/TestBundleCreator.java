/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.osgi.framework.Version;

public class TestBundleCreator extends TestCase {

	private static final String TEST_BUNDLE_NAME = "TestBundle_";

	// Commented out to prevent it being run as a JUnit test
	//	public void testCreatePlugins() throws CoreException {
	//		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	//		IPath path = root.getLocation();
	//		createPlugins(path.toFile(), 1000);
	//	}

	public static void createPlugins(File root, int count) throws CoreException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IBundleProjectService service = PDECore.getDefault().acquireService(IBundleProjectService.class);
		List<IProject> projects = new ArrayList<>();

		// Disable building until the end
		try {
			IWorkspaceDescription d = ws.getDescription();
			d.setAutoBuilding(false);
			ws.setDescription(d);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// Create the projects in the workspace
		for (int i = 1; i <= count; i++) {
			String projectName = TEST_BUNDLE_NAME + Integer.toString(i);
			IProject proj = ws.getRoot().getProject(projectName);
			assertFalse("Project should not exist", proj.exists());
			IBundleProjectDescription description = service.getDescription(proj);

			description.setSymbolicName(projectName);
			description.setBundleVersion(new Version(Integer.toString(i)));

			// Add required bundle for each of the previously created bundles

			List<IRequiredBundleDescription> requiredBundles = new ArrayList<>();
			for (int j = 1; j < Math.max(j, 200); j++) {
				IRequiredBundleDescription req = service.newRequiredBundle(TEST_BUNDLE_NAME + Integer.toString(j), null, false, false);
				requiredBundles.add(req);
			}
			description.setRequiredBundles(requiredBundles.toArray(new IRequiredBundleDescription[requiredBundles.size()]));

			//			service.newPackageImport(name, range, optional);
			//			description.setPackageImports(imports)

			description.apply(null);

			projects.add(proj);
		}

		// Build all the bundles so their models are available to export
		ws.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Export the project in a jar
		final FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = true;
		info.useJarFormat = true;
		info.exportSource = false;
		info.allowBinaryCycles = false;
		info.useWorkspaceCompiledClasses = false;
		info.zipFileName = null;
		info.signingInfo = null;
		info.qualifier = "v" + System.currentTimeMillis();
		info.destinationDirectory = root.getAbsolutePath();
		List<IPluginModelBase> bundles = new ArrayList<>();
		for (IProject proj : projects) {
			bundles.add(PluginRegistry.findModel(proj));
		}
		info.items = bundles.toArray();

		PluginExportOperation job = new PluginExportOperation(info, "Export Bundles");
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
		}

		// Delete all the projects
		for (IProject proj : projects) {
			proj.delete(true, true, null);
		}

	}

}
