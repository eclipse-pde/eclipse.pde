/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Test;

/**
 * Tests projects with a custom execution environment
 */
public class ExecutionEnvironmentTests extends PDETestCase {

	/**
	 * Deletes the specified project.
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
	 */
	protected void validateOption(IJavaProject project, String optionName, String expectedValue) {
		String option = project.getOption(optionName, true);
		assertEquals("Wrong value for option " + optionName, expectedValue, option);
	}

	/**
	 * Validates the JRE class path container is as expected.
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
	 * @param classfile
	 *            location of class file in local file system
	 * @param major
	 *            expected major class file version
	 */
	protected void validateTargetLevel(String classfile, int major) {
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(classfile, IClassFileReader.ALL);
		assertEquals("Wrong major version", major, reader.getMajorVersion());
	}

	/**
	 * Creates a plug-in project with a custom execution environment. Validates
	 * that compiler compliance settings and build path are correct and that
	 * class files are generated with correct target level.
	 *
	 * TODO The VM this is run on must be included in the compatible JREs for
	 * the custom environment. See
	 * {@link EnvironmentAnalyzerDelegate#analyze(org.eclipse.jdt.launching.IVMInstall, IProgressMonitor)}
	 */
	@Test
	public void testCustomEnvironment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager()
					.getEnvironment(EnvironmentAnalyzerDelegate.EE_NO_SOUND);
			IJavaProject project = ProjectUtils.createPluginProject("no.sound", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// ensure class files are build with correct target level
			project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			waitForBuild();
			IFile file = project.getProject().getFile("/bin/no/sound/Activator.class");
			assertTrue("Activator class missing", file.exists());
			validateTargetLevel(file.getLocation().toOSString(), ClassFileConstants.MAJOR_VERSION_1_8);
		} finally {
			deleteProject("no.sound");
		}
	}

	/**
	 * Creates a plug-in project with a JavaSE-17 execution environment.
	 * Validates that compiler compliance settings and build path are correct
	 * and that class files are generated with correct target level.
	 */
	@Test
	public void testJava8Environment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-1.8");
			IJavaProject project = ProjectUtils.createPluginProject("j2se18.plug", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			waitForBuild();
			IFile file = project.getProject().getFile("/bin/j2se18/plug/Activator.class");
			assertTrue("Activator class missing", file.exists());
			validateTargetLevel(file.getLocation().toOSString(), ClassFileConstants.MAJOR_VERSION_1_8);
		} finally {
			deleteProject("j2se18.plug");
		}
	}

	/**
	 * Creates a plug-in project without an execution environment. Validates
	 * that compiler compliance settings and build path reflect default
	 * workspace settings.
	 */
	@Test
	public void testNoEnvironment() throws Exception {
		try {
			IJavaProject project = ProjectUtils.createPluginProject("no.env", (IExecutionEnvironment) null);
			assertTrue("Project was not created", project.exists());

			Hashtable<String, String> options = JavaCore.getOptions();
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
					options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
			validateOption(project, JavaCore.COMPILER_SOURCE, options.get(JavaCore.COMPILER_SOURCE));
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, options.get(JavaCore.COMPILER_COMPLIANCE));
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER,
					options.get(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER));
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER,
					options.get(JavaCore.COMPILER_PB_ENUM_IDENTIFIER));

			validateSystemLibrary(project, JavaRuntime.newDefaultJREContainerPath());
		} finally {
			deleteProject("no.env");
		}
	}

	/**
	 * Creates a plug-in project with a JavaSE-1.8 execution environment.
	 * Validates that compiler compliance settings and build path are correct.
	 * Modifies the compliance options and then updates the class path again.
	 * Ensures that the enum and assert identifier options get overwritten with
	 * minimum 'warning' severity.
	 */
	@Test
	public void testMinimumComplianceOverwrite() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-1.8");
			IJavaProject project = ProjectUtils.createPluginProject("j2se18.ignore", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// set to ignore assert/enum options
			project.setOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			project.setOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);

			// updating class path should increase severity to error
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			UpdateClasspathJob.scheduleFor(List.of(model), false).join();
			// re-validate options
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

		} finally {
			deleteProject("j2se18.ignore");
		}
	}

	/**
	 * Creates a plug-in project with a JavaSE-1.8 execution environment.
	 * Validates that compiler compliance settings and build path are correct.
	 * Modifies the compliance options and then updates the class path again.
	 * Ensures that the enum and assert identifier options do not overwrite
	 * existing 'error' severity.
	 */
	@Test
	public void testMinimumComplianceNoOverwrite() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-1.8");
			IJavaProject project = ProjectUtils.createPluginProject("j2se18.error", env);
			assertTrue("Project was not created", project.exists());

			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			validateSystemLibrary(project, JavaRuntime.newJREContainerPath(env));

			// set to ignore assert/enum options
			project.setOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			project.setOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

			// updating class path should increase severity to error
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			UpdateClasspathJob.scheduleFor(List.of(model), false).join();

			// re-validate options
			validateOption(project, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			validateOption(project, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			validateOption(project, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

		} finally {
			deleteProject("j2se18.error");
		}
	}

	@Test
	public void testDynamicSystemPackages() throws Exception {
		IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-11");
		String systemPackages = TargetPlatformHelper.getSystemPackages(env, null);
		assertThat(systemPackages).isNotNull();
		assertThat(systemPackages.split(",")).contains("java.lang", "javax.sql", "org.w3c.dom.css");
	}
}
