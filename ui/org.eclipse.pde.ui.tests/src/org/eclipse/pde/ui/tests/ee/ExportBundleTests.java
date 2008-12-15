/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.ee;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.ProjectUtils;

/**
 * Tests exporting bundles.
 */
public class ExportBundleTests extends PDETestCase {
	
	private static final IPath EXPORT_PATH = MacroPlugin.getDefault().getStateLocation().append(".export");
	
	public static Test suite() {
		return new TestSuite(ExportBundleTests.class);
	}	
	
	/**
	 * Deletes the specified project.
	 * 
	 * @param name
	 * @throws CoreException
	 */
	protected void deleteProject(String name) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project.exists()) {
			project.delete(true, null);
		}
	}
	
	/**
	 * Validates the target level of a generated class file.
	 * 
	 * @param zipFileName location of archive file
	 * @param zipEntryName path to class file in archive
	 * @param major expected major class file version
	 */
	protected void validateTargetLevel(String zipFileName, String zipEntryName, int major) {
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(zipFileName, zipEntryName, IClassFileReader.ALL);
		assertEquals("Wrong major version", major, reader.getMajorVersion());
	}	
	
	/**
	 * Exports a plug-in project with a custom execution environment and validates class file
	 * target level.
	 * 
	 * @throws Exception
	 */
	public void testExportCustomEnvironment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(EnvironmentAnalyzerDelegate.EE_NO_SOUND);
	        IJavaProject project = ProjectUtils.createPluginProject("no.sound.export", env);
	        assertTrue("Project was not created", project.exists());
	        
			final FeatureExportInfo info = new FeatureExportInfo();
			info.toDirectory = true;
			info.useJarFormat = true;
			info.exportSource = false;
			info.allowBinaryCycles = false;
			info.destinationDirectory = EXPORT_PATH.toOSString();
			info.zipFileName = null;
			info.items = new Object[]{PluginRegistry.findModel(project.getProject())};
			info.signingInfo = null;
			info.qualifier = "vXYZ";
			
			PluginExportOperation job = new PluginExportOperation(info, "Test-Export");
			job.schedule();
			job.join();
			if (job.hasAntErrors()){
				fail("Export job had ant errors");
			}
			IStatus result = job.getResult();
			assertTrue("Export job had errors", result.isOK());
			
			// veriry exported bundle exists
			IPath path = EXPORT_PATH.append("plugins/no.sound.export_1.0.0.jar");
			assertTrue("Missing exported bundle", path.toFile().exists());
			validateTargetLevel(path.toOSString(), "no/sound/export/Activator.class", 47);
		} finally {
			deleteProject("no.sound.export");
		}
	}
		
	/**
	 * Exports a plug-in project with a J2SE-1.4 execution environment and validates class file
	 * target level.
	 * 
	 * @throws Exception
	 */
	public void testExport14Environment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4");
	        IJavaProject project = ProjectUtils.createPluginProject("j2se14.export", env);
	        assertTrue("Project was not created", project.exists());
	        
			final FeatureExportInfo info = new FeatureExportInfo();
			info.toDirectory = true;
			info.useJarFormat = true;
			info.exportSource = false;
			info.allowBinaryCycles = false;
			info.destinationDirectory = EXPORT_PATH.toOSString();
			info.zipFileName = null;
			info.items = new Object[]{PluginRegistry.findModel(project.getProject())};
			info.signingInfo = null;
			info.qualifier = "vXYZ";
			
			PluginExportOperation job = new PluginExportOperation(info, "Test-Export");
			job.schedule();
			job.join();
			if (job.hasAntErrors()){
				fail("Export job had ant errors");
			}
			IStatus result = job.getResult();
			assertTrue("Export job had errors", result.isOK());
			
			// veriry exported bundle exists
			IPath path = EXPORT_PATH.append("plugins/j2se14.export_1.0.0.jar");
			assertTrue("Missing exported bundle", path.toFile().exists());
			validateTargetLevel(path.toOSString(), "j2se14/export/Activator.class", 46);
		} finally {
			deleteProject("j2se14.export");
		}
	}
			
}
