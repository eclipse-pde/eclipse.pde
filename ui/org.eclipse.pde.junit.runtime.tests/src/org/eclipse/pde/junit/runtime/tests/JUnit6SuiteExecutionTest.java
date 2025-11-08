/*******************************************************************************
 *  Copyright (c) 2025 Julian Honnen
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
import static org.eclipse.pde.junit.runtime.tests.JUnitExecutionTest.findType;

import java.util.StringJoiner;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class JUnit6SuiteExecutionTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static IJavaProject project;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Assert.assertNotNull("junit-platform-suite-engine bundle missing", Platform.getBundle("junit-platform-suite-engine"));
		Assert.assertNotNull("org.eclipse.jdt.junit6.runtime bundle missing", Platform.getBundle("org.eclipse.jdt.junit6.runtime"));

		JUnitExecutionTest.setupProjects();
		project = JUnitExecutionTest.getJProject("verification.tests.junit6.suite");
	}

	@Test
	public void executeSuite() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(findType(project, "TestSuite"));
		JUnitExecutionTest.assertSuccessful(session);
		Assert.assertEquals("""
				verification.tests.junit6.suite.TestSuite
				  JUnit Jupiter
				    verification.tests.junit6.Test1
				      test1(verification.tests.junit6.Test1)
				      test2(verification.tests.junit6.Test1)
				    verification.tests.junit6.Test2
				      test(verification.tests.junit6.Test2)
				""".strip(), toString(session).strip());
	}

	@Test
	public void executePackage() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(findType(project, "TestSuite").getPackageFragment());
		JUnitExecutionTest.assertSuccessful(session);
		assertThat(session.getChildren()).isNotEmpty();
	}

	@Test
	public void executeProject() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(project);
		JUnitExecutionTest.assertSuccessful(session);
		assertThat(session.getChildren()).isNotEmpty();
	}

	private static String toString(ITestRunSession session) {
		StringJoiner sb = new StringJoiner("\n");
		for (ITestElement element : session.getChildren()) {
			append(sb, element, 0);
		}
		return sb.toString();
	}

	private static void append(StringJoiner sb, ITestElement element, int indent) {
		sb.add("  ".repeat(indent) + ((TestElement) element).getTestName());
		if (element instanceof ITestElementContainer container) {
			for (ITestElement child : container.getChildren()) {
				append(sb, child, indent + 1);
			}
		}
	}

}
