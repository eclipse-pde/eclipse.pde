/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.anttasks.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

public abstract class AntRunnerTestCase {
	public static final String PROJECT_NAME = "pde.apitools"; //$NON-NLS-1$

	private static final String BUILD_EXCEPTION_CLASS_NAME = "org.apache.tools.ant.BuildException"; //$NON-NLS-1$

	private IFolder buildFolder = null;

	public abstract String getTestResourcesFolder();

	protected IProject newTest() throws Exception {
		IProject builderProject = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!builderProject.exists()) {
			builderProject.create(null);
		}
		if (!builderProject.isOpen()) {
			builderProject.open(null);
		}

		return builderProject;
	}

	protected IFolder newTest(String resources) throws Exception {
		IProject builderProject = newTest();

		// create build folder for this test
		IPath resourcePath = new Path(resources);
		for (int i = 0, max = resourcePath.segmentCount(); i < max; i++) {
			String segment = resourcePath.segment(i);
			if (i == 0) {
				this.buildFolder = builderProject.getFolder(segment);
			} else {
				this.buildFolder = this.buildFolder.getFolder(segment);
			}
			if (this.buildFolder.exists()) {
				try {
					this.buildFolder.delete(true, null);
					this.buildFolder.create(true, true, null);
				} catch (CoreException e) {
				}
			} else {
				this.buildFolder.create(true, true, null);
			}
		}

		IPath pluginDirectoryPath = TestSuiteHelper.getPluginDirectoryPath();
		String path = pluginDirectoryPath.append(new Path("/test-anttasks/" + resources)).toOSString(); //$NON-NLS-1$
		File sourceFile = new File(path);
		if (!sourceFile.exists()) {
			System.err.println("Source folder " + path + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
			return buildFolder;
		} else if (!sourceFile.isDirectory()) {
			System.err.println("Source folder " + path + " is not a folder"); //$NON-NLS-1$ //$NON-NLS-2$
			return buildFolder;
		}
		path = buildFolder.getLocation().toOSString();
		File destinationFile = new File(path);
		if (!destinationFile.exists()) {
			System.err.println("Destination folder " + path + " is missing"); //$NON-NLS-1$ //$NON-NLS-2$
			return buildFolder;
		} else if (!destinationFile.isDirectory()) {
			System.err.println("Destination folder " + path + " is not a folder"); //$NON-NLS-1$ //$NON-NLS-2$
			return buildFolder;
		}
		TestSuiteHelper.copy(sourceFile, destinationFile);
		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

		return buildFolder;
	}

	protected IFolder newTest(String parentFolder, String[] resources) throws Exception {
		buildFolder = newTest(parentFolder + resources[0]);

		if (resources.length > 1) {
			for (int index = 1; index < resources.length; ++index) {
				IPath pluginDirectoryPath = TestSuiteHelper.getPluginDirectoryPath();
				String path = pluginDirectoryPath.append(new Path("/test-anttasks/" + parentFolder + resources[index])).toOSString(); //$NON-NLS-1$
				File sourceDataFile = new File(path);
				path = buildFolder.getLocation().toOSString();
				File destinationDataFile = new File(path);
				TestSuiteHelper.copy(sourceDataFile, destinationDataFile);
			}
		}
		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		return buildFolder;

	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties) throws Exception {
		runAntScript(script, targets, antHome, additionalProperties, null, null);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties, String listener, String logger) throws Exception {
		String[] args = createAntRunnerArgs(script, targets, antHome, additionalProperties, listener, logger);
		try {
			AntRunner runner = new AntRunner();
			runner.run(args);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof Exception) {
				throw (Exception) target;
			}
			throw e;
		} finally {
			this.buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
	}

	protected String[] createAntRunnerArgs(String script, String[] targets, String antHome, Properties additionalProperties, String listener, String logger) {
		int numArgs = 5 + targets.length + (additionalProperties != null ? additionalProperties.size() : 0);
		if (listener != null) {
			numArgs += 2;
		}
		if (logger != null) {
			numArgs += 2;
		}
		String[] args = new String[numArgs];
		int idx = 0;
		args[idx++] = "-buildfile"; //$NON-NLS-1$
		args[idx++] = script;
		args[idx++] = "-logfile"; //$NON-NLS-1$
		args[idx++] = antHome + "/log.log"; //$NON-NLS-1$
		args[idx++] = "-Dbuilder=" + antHome; //$NON-NLS-1$
		if (listener != null) {
			args[idx++] = "-listener"; //$NON-NLS-1$
			args[idx++] = listener;
		}
		if (logger != null) {
			args[idx++] = "-logger"; //$NON-NLS-1$
			args[idx++] = logger;
		}
		if (additionalProperties != null && additionalProperties.size() > 0) {
			Enumeration<Object> e = additionalProperties.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = additionalProperties.getProperty(key);
				if (!value.isEmpty()) {
					args[idx++] = "-D" + key + "=" + additionalProperties.getProperty(key); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					args[idx++] = ""; //$NON-NLS-1$
				}
			}
		}

		for (String target : targets) {
			args[idx++] = target;
		}
		return args;
	}

	public void checkBuildException(Exception e) {
		assertEquals("Not BuildException", BUILD_EXCEPTION_CLASS_NAME, e.getClass().getCanonicalName()); //$NON-NLS-1$
	}
}