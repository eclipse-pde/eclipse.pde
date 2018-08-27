/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.ee;

import java.util.Hashtable;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.ProjectUtils;

/**
 * Tests projects with a custom execution environment
 */
public class ExecutionEnvironmentTests extends PDETestCase {

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
	 * Wait for builds to complete
	 */
	public void waitForBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}

	/**
	 * Validates that the project's option is as expected
	 *
	 * @param optionName
	 * @param expectedValue
	 */
	protected void validateOption(IJavaProject project, String optionName, String expectedValue) {
		String option = project.getOption(optionName, true);
		assertEquals("Wrong value for option " + optionName, expectedValue, option);
	}

	/**
	 * Validates the JRE class path container is as expected.
	 *
	 * @param project
	 * @param conatinerPath
	 * @throws JavaModelException
	 */
	protected void validateSystemLibrary(IJavaProject project, IPath conatinerPath) throws JavaModelException {
		IClasspathEntry[] classpath = project.getRawClasspath();
		for (IClasspathEntry entry : classpath) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				IPath path = entry.getPath();
				if (JavaRuntime.JRE_CONTAINER.equals(path.segment(0))) {
					assertEquals("Wrong system library class path container", conatinerPath, path);
					return;
				}
			}
		}
		assertFalse("JRE container not found", true);
	}

	/**
	 * Validates the target level of a generated class file.
	 *
	 * @param classfile location of class file in local file system
	 * @param major expected major class file version
	 */
	protected void validateTargetLevel(String classfile, int major) {
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(classfile, IClassFileReader.ALL);
		assertEquals("Wrong major version", major, reader.getMajorVersion());
	}

	/**
	 * Creates a plug-in project with a custom execution environment. Validates that
	 * compiler compliance settings and build path are correct and that class files
	 * are generated with correct target level.
	 *
	 * TODO The VM this is run on must be included in the compatible JREs for the custom
	 * environment. See {@link EnvironmentAnalyzerDelegate#analyze(org.eclipse.jdt.launching.IVMInstall, IProgressMonitor)}
	 *
	 * @throws Exception
	 */
	public void testCustomEnvironment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(EnvironmentAnalyzerDelegate.EE_NO_SOUND);
			IJavaProject project = ProjectUtils.createPluginProject("no.sound", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// ensure class files are build with correct target level
			project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			waitForBuild();
			IFile file = project.getProject().getFile("/bin/no/sound/Activator.class");
			assertTrue("Activator class missing", file.exists());
			validateTargetLevel(file.getLocation().toOSString(), 47);
		} finally {
			deleteProject("no.sound");
		}
	}

	/**
	 * Creates a plug-in project with a J2SE-1.4 execution environment. Validates that
	 * compiler compliance settings and build path are correct and that class files
	 * are generated with correct target level.
	 *
	 * @throws Exception
	 */
	public void testJava4Environment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4");
			IJavaProject project = ProjectUtils.createPluginProject("j2se14.plug", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			waitForBuild();
			IFile file = project.getProject().getFile("/bin/j2se14/plug/Activator.class");
			assertTrue("Activator class missing", file.exists());
			validateTargetLevel(file.getLocation().toOSString(), 46);
		} finally {
			deleteProject("j2se14.plug");
		}
	}

	/**
	 * Creates a plug-in project without an execution environment. Validates that
	 * compiler compliance settings and build path reflect default workspace settings.
	 *
	 * @throws Exception
	 */
	public void testNoEnvironment() throws Exception {
		try {
			IJavaProject project = ProjectUtils.createPluginProject("no.env", null);
			assertTrue("Project was not created", project.exists());

			Hashtable<String, String> options = JavaCore.getOptions();
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
			validateOption(project, JavaCore.COMPILER_SOURCE, options.get(JavaCore.COMPILER_SOURCE));
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, options.get(JavaCore.COMPILER_COMPLIANCE));
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, options.get(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER));
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, options.get(JavaCore.COMPILER_PB_ENUM_IDENTIFIER));

			validateSystemLibrary(project, JavaRuntime.newDefaultJREContainerPath());
		} finally {
			deleteProject("no.env");
		}
	}

	/**
	 * Creates a plug-in project with a J2SE-1.4 execution environment. Validates that
	 * compiler compliance settings and build path are correct. Modifies the compliance
	 * options and then updates the class path again. Ensures that the enum and assert
	 * identifier options get overwritten with minimum 'warning' severity.
	 *
	 * @throws Exception
	 */
	public void testMinimumComplianceOverwrite() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4");
			IJavaProject project = ProjectUtils.createPluginProject("j2se14.ignore", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// set to ignore assert/enum options
			project.setOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			project.setOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);

			// updating class path should increase severity to warning
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			UpdateClasspathJob job = new UpdateClasspathJob(new IPluginModelBase[]{model});
			job.schedule();
			job.join();

			// re-validate options
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);

		} finally {
			deleteProject("j2se14.ignore");
		}
	}

	/**
	 * Creates a plug-in project with a J2SE-1.4 execution environment. Validates that
	 * compiler compliance settings and build path are correct. Modifies the compliance
	 * options and then updates the class path again. Ensures that the enum and assert
	 * identifier options do not overwrite existing 'error' severity.
	 *
	 * @throws Exception
	 */
	public void testMinimumComplianceNoOverwrite() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4");
			IJavaProject project = ProjectUtils.createPluginProject("j2se14.error", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// set to ignore assert/enum options
			project.setOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			project.setOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			// updating class path should increase severity to warning
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			UpdateClasspathJob job = new UpdateClasspathJob(new IPluginModelBase[]{model});
			job.schedule();
			job.join();

			// re-validate options
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

		} finally {
			deleteProject("j2se14.error");
		}
	}
}
