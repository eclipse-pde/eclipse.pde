/*******************************************************************************
 *  Copyright (c) 2019, 2025 Julian Honnen
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
import java.util.Collections;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
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
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@RunWith(Parameterized.class)
public class JUnitExecutionTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Pattern junitRuntimeIDs = Pattern
				.compile("\\.junit\\d*\\.runtime|junit\\..+\\.engine$|org\\.junit\\.platform\\.launcher");
		TargetPlatformUtil.setRunningPlatformSubSetAsTarget(TargetPlatformUtil.class + "_target", b -> {
			// filter out junit.runtime and test engine bundles from the target platform
			// this tests the scenario where PDE supplies them from the host installation
			return !junitRuntimeIDs.matcher(b.getSymbolicName()).find();
		});

		Bundle bundle = FrameworkUtil.getBundle(JUnitExecutionTest.class);

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (URL resource : Collections.list(bundle.findEntries("test-bundles", "verification.tests.*", false))) {
			ProjectUtils.importTestProject(FileLocator.toFileURL(resource));
		}
		TestUtils.waitForJobs(JUnitExecutionTest.class + ".setupProjects() before build", 100, 10_000);
		workspaceRoot.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestExecutionUtil.waitForAutoBuild();
		TestUtils.waitForJobs(JUnitExecutionTest.class + ".setupProjects() after build", 100, 10_000);
	}

	@Parameters(name = "{0}")
	public static Object[][] parameters() {
		return new Object[][] {
				{ "JUnit6", getJProject("verification.tests.junit6") },
				{ "JUnit6 Fragment", getJProject("verification.tests.junit6.fragment") },
				{ "JUnit5", getJProject("verification.tests.junit5") },
				{ "JUnit5 Fragment", getJProject("verification.tests.junit5.fragment") },
				{ "JUnit4", getJProject("verification.tests.junit4") },
				{ "JUnit4 Fragment", getJProject("verification.tests.junit4.fragment") },
				{ "JUnit4 (JUnitPlatform)", getJProject("verification.tests.junit4.platform") },
				{ "JUnit4 (JUnitPlatform) Fragment", getJProject("verification.tests.junit4.platform.fragment") },
				{ "Java 11 bundle with module limit", getJProject("verification.tests.limitmodules") },
				{ "Using a 'test' source folder", getJProject("verification.tests.testfolder") } };
	}

	static IJavaProject getJProject(String projectName) {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
	}

	@Parameter(0)
	public String testCaseName; // Just for display
	@Parameter(1)
	public IJavaProject project;

	@Test
	public void executeType() throws Exception {
		IType testClass = findType(project, "Test1");
		ITestRunSession session = TestExecutionUtil.runTest(testClass);

		assertSuccessful(session);
		assertThat(session.getChildren()).hasSize(1);
	}

	@Test
	public void executePackage() throws Exception {
		IPackageFragment testPackage = findType(project, "Test1").getPackageFragment();
		ITestRunSession session = TestExecutionUtil.runTest(testPackage);

		assertSuccessful(session);
		assertThat(session.getChildren()).hasSize(2);
	}

	@Test
	public void executeProject() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(project);

		assertSuccessful(session);
		assertThat(session.getChildren()).hasSize(2);
	}

	@Test
	public void executeMethod() throws Exception {
		IMethod testMethod = findType(project, "Test1").getMethod("test1", new String[0]);
		Assume.assumeTrue(testMethod.exists());
		ITestRunSession session = TestExecutionUtil.runTest(testMethod);

		assertSuccessful(session);
		assertThat(session.getChildren()).hasSize(1);
	}

	static void assertSuccessful(ITestRunSession session) {
		Result testResult = session.getTestResult(true);
		if (ITestElement.Result.OK.equals(testResult)) {
			return;
		}

		AssertionError assertionFailedError = new AssertionError("test completed with " + testResult);
		addFailureTraces(session, assertionFailedError);

		throw assertionFailedError;
	}

	private static void addFailureTraces(ITestElement element, AssertionError assertionFailedError) {
		FailureTrace trace = element.getFailureTrace();
		if (trace != null) {
			assertionFailedError
					.addSuppressed(new AssertionError("FailureTrace of " + element + ":\n\n" + trace.getTrace()));
		}

		if (element instanceof ITestElementContainer) {
			for (ITestElement child : ((ITestElementContainer) element).getChildren()) {
				addFailureTraces(child, assertionFailedError);
			}
		}
	}

	static IType findType(IJavaProject project, String name) throws JavaModelException {
		IType type = project.findType(project.getElementName(), name);
		assertNotNull(type);
		Assert.assertTrue(type.exists());
		return type;
	}
}
