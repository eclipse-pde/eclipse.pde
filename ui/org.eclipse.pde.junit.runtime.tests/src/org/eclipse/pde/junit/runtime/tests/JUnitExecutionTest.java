/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.junit.runtime.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@RunWith(Parameterized.class)
public class JUnitExecutionTest {

	@Parameter
	public TestInput input;

	@BeforeClass
	public static void setupProjects() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();

		Bundle bundle = FrameworkUtil.getBundle(JUnitExecutionTest.class);

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (URL resource : Collections.list(bundle.findEntries("test-bundles", "*", false))) {
			IPath resourcePath = IPath.fromOSString(FileLocator.toFileURL(resource).getPath());
			IPath descriptionPath = resourcePath.append(IProjectDescription.DESCRIPTION_FILE_NAME);
			if (!descriptionPath.toFile().exists())
				continue;

			IProjectDescription projectDescription = workspaceRoot.getWorkspace()
					.loadProjectDescription(descriptionPath);
			IProject project = workspaceRoot.getProject(resourcePath.lastSegment());
			if (!project.exists()) {
				project.create(projectDescription, null);
				project.open(null);
			}
		}
		
		workspaceRoot.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	@Parameters(name = "{0}")
	public static Collection<TestInput> parameters() {
		return Arrays.asList(
				new TestInput("JUnit5", "verification.tests.junit5"),
				new TestInput("JUnit5 Fragment", "verification.tests.junit5.fragment"),
				new TestInput("JUnit4", "verification.tests.junit4"),
				new TestInput("JUnit4 Fragment", "verification.tests.junit4.fragment"),
				new TestInput("JUnit4 (JUnitPlatform)", "verification.tests.junit4.platform"),
				new TestInput("JUnit4 (JUnitPlatform) Fragment", "verification.tests.junit4.platform.fragment"),
				new TestInput("Java 11 bundle with module limit", "verification.tests.limitmodules"),
				new TestInput("Using a 'test' source folder", "verification.tests.testfolder")
				);
	}

	@Test
	public void executeType() throws Exception {
		IType testClass = input.findType("Test1");
		ITestRunSession session = TestExecutionUtil.runTest(testClass);

		assertThat(session.getChildren()).hasSize(1);
		assertSuccessful(session);
	}

	@Test
	public void executePackage() throws Exception {
		IPackageFragment testPackage = input.findType("Test1").getPackageFragment();
		ITestRunSession session = TestExecutionUtil.runTest(testPackage);

		assertThat(session.getChildren()).hasSize(2);
		assertSuccessful(session);
	}

	@Test
	public void executeProject() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(input.project);

		assertThat(session.getChildren()).hasSize(2);
		assertSuccessful(session);
	}

	@Test
	public void executeMethod() throws Exception {
		IMethod testMethod = input.findType("Test1").getMethod("test1", new String[0]);
		Assume.assumeTrue(testMethod.exists());
		ITestRunSession session = TestExecutionUtil.runTest(testMethod);

		assertThat(session.getChildren()).hasSize(1);
		assertSuccessful(session);
	}

	static void assertSuccessful(ITestRunSession session) {
		Result testResult = session.getTestResult(true);
		if (ITestElement.Result.OK.equals(testResult))
			return;

		AssertionError assertionFailedError = new AssertionError("test completed with " + testResult);
		addFailureTraces(session, assertionFailedError);

		throw assertionFailedError;
	}

	private static void addFailureTraces(ITestElement element, AssertionError assertionFailedError) {
		FailureTrace trace = element.getFailureTrace();
		if (trace != null) {
			assertionFailedError.addSuppressed(new AssertionError("FailureTrace of " + element + ":\n\n" + trace.getTrace()));
		}

		if (element instanceof ITestElementContainer) {
			for (ITestElement child : ((ITestElementContainer) element).getChildren()) {
				addFailureTraces(child, assertionFailedError);
			}
		}
	}

	static class TestInput {
		private final String displayName;

		final IJavaProject project;

		public TestInput(String displayName, String projectName) {
			this.displayName = displayName;
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			this.project = JavaCore.create(project);
		}

		@Override
		public String toString() {
			return displayName;
		}

		public IType findType(String name) throws JavaModelException {
			IType type = project.findType(project.getElementName(), name);
			assertNotNull(type);
			Assert.assertTrue(type.exists());
			return type;
		}
	}

}
