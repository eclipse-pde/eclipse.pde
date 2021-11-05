/*******************************************************************************
 *  Copyright (c) 2021 Julian Honnen
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.pde.junit.runtime.tests.JUnitExecutionTest.TestInput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JUnit5SuiteExecutionTest {

	private static TestInput input;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Assert.assertNotNull("org.junit.platform.suite.engine bundle missing", Platform.getBundle("org.junit.platform.suite.engine"));
		
		JUnitExecutionTest.setupProjects();
		input = new TestInput("JUnit5 Suite", "verification.tests.junit5.suite");
	}

	@Test
	public void executeSuite() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(input.findType("TestSuite"));

		JUnitExecutionTest.assertSuccessful(session);
		Assert.assertEquals(
				"verification.tests.junit5.suite.TestSuite\n"
				+ "  JUnit Jupiter\n"
				+ "    verification.tests.junit5.Test1\n"
				+ "      test1(verification.tests.junit5.Test1)\n"
				+ "      test2(verification.tests.junit5.Test1)\n"
				+ "    verification.tests.junit5.Test2\n"
				+ "      test(verification.tests.junit5.Test2)\n",
				toString(session));
	}

	@Test
	public void executePackage() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(input.findType("TestSuite").getPackageFragment());
		JUnitExecutionTest.assertSuccessful(session);
		assertThat(session.getChildren()).isNotEmpty();
	}

	@Test
	public void executeProject() throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(input.project);
		JUnitExecutionTest.assertSuccessful(session);
		assertThat(session.getChildren()).isNotEmpty();
	}

	private static String toString(ITestRunSession session) {
		StringBuilder sb = new StringBuilder();
		for (ITestElement element : session.getChildren()) {
			append(sb, element, 0);
		}

		return sb.toString();
	}

	private static void append(StringBuilder sb, ITestElement element, int indent) {
		sb.append("  ".repeat(indent));
		sb.append(((TestElement) element).getTestName());
		sb.append('\n');

		if (element instanceof ITestElementContainer) {
			for (ITestElement child : ((ITestElementContainer) element).getChildren()) {
				append(sb, child, indent + 1);
			}
		}
	}

}
