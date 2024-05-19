/*******************************************************************************
 *  Copyright (c) 2021, 2022 Julian Honnen
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JUnit5SuiteExecutionTest {

	private static IJavaProject project;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Assert.assertNotNull("junit-platform-suite-engine bundle missing", Platform.getBundle("junit-platform-suite-engine"));
		Assert.assertNotNull("org.eclipse.jdt.junit5.runtime bundle missing", Platform.getBundle("org.eclipse.jdt.junit5.runtime"));

		JUnitExecutionTest.setupProjects();
		project = JUnitExecutionTest.getJProject("verification.tests.junit5.suite");
	}

	@Test
	public void executeSuite() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(findType(project, "TestSuite"));
		JUnitExecutionTest.assertSuccessful(session);
		Assert.assertEquals("""
				verification.tests.junit5.suite.TestSuite
				  JUnit Jupiter
				    verification.tests.junit5.Test1
				      test1(verification.tests.junit5.Test1)
				      test2(verification.tests.junit5.Test1)
				    verification.tests.junit5.Test2
				      test(verification.tests.junit5.Test2)
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
