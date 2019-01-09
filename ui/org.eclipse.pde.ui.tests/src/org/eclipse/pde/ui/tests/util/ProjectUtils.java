/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginFieldData;
import org.eclipse.pde.ui.IBundleContentWizard;
import org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.tests.runtime.TestUtils;

/**
 * Utility class for project related operations
 */
public class ProjectUtils {

	/**
	 * Used to create projects
	 */
	static class TestProjectProvider implements IProjectProvider {
		private String fProjectName;

		TestProjectProvider(String projectName) {
			fProjectName = projectName;
		}
		@Override
		public IPath getLocationPath() {
			return ResourcesPlugin.getWorkspace().getRoot().getLocation();
		}

		@Override
		public IProject getProject() {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(fProjectName);
		}

		@Override
		public String getProjectName() {
			return fProjectName;
		}

	}

	/**
	 * Fake wizard
	 */
	static class TestBundleWizard extends AbstractNewPluginTemplateWizard {

		@Override
		protected void addAdditionalPages() {
		}

		@Override
		public ITemplateSection[] getTemplateSections() {
			return new ITemplateSection[0];
		}

	}

	/**
	 * Constant representing the name of the output directory for a project.
	 * Value is: <code>bin</code>
	 */
	public static final String BIN_FOLDER = "bin";

	/**
	 * Constant representing the name of the source directory for a project.
	 * Value is: <code>src</code>
	 */
	public static final String SRC_FOLDER = "src";

	/**
	 * Create a plugin project with the given name and execution environment.
	 *
	 * @param projectName
	 * @param env environment for build path or <code>null</code> if default system JRE
	 * @return a new plugin project
	 * @throws CoreException
	 */
	public static IJavaProject createPluginProject(String projectName, IExecutionEnvironment env) throws Exception {
		PluginFieldData data = new PluginFieldData();
		data.setName(projectName);
		data.setId(projectName);
		data.setLegacy(false);
		data.setHasBundleStructure(true);
		data.setSimple(false);
		data.setProvider("IBM");
		data.setLibraryName(".");
		data.setVersion("1.0.0");
		data.setTargetVersion("3.5");
		data.setOutputFolderName(BIN_FOLDER);
		data.setSourceFolderName(SRC_FOLDER);
		if (env != null) {
			data.setExecutionEnvironment(env.getId());
		}
		data.setDoGenerateClass(true);
		data.setClassname(projectName + ".Activator");
		data.setEnableAPITooling(false);
		data.setRCPApplicationPlugin(false);
		data.setUIPlugin(false);
		IProjectProvider provider = new TestProjectProvider(projectName);
		IBundleContentWizard wizard = new TestBundleWizard();
		NewProjectCreationOperation operation = new NewProjectCreationOperation(data, provider, wizard);
		operation.run(new NullProgressMonitor());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IJavaProject javaProject = JavaCore.create(project);
		TestUtils.waitForJobs("ProjectUtils.createPluginProject " + projectName, 100, 10000);
		return javaProject;
	}


}
