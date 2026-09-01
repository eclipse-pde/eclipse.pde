/*******************************************************************************
 *  Copyright (c) 2021, 2022, 2025 Julian Honnen
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

import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.pde.junit.runtime.tests.JUnitExecutionTest.findType;
import static org.eclipse.pde.junit.runtime.tests.JUnitExecutionTest.getJProject;

import java.util.StringJoiner;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JUnitSuiteExecutionTest {

	@RegisterExtension
	public static final Extension CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeAll
	public static void setupProjects() throws Exception {
		Assertions.assertNotNull(Platform.getBundle("junit-platform-suite-engine"), "junit-platform-suite-engine bundle missing"); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertNotNull(Platform.getBundle("org.eclipse.jdt.junit5.runtime"), "org.eclipse.jdt.junit5.runtime bundle missing"); //$NON-NLS-1$ //$NON-NLS-2$
		Assertions.assertNotNull(Platform.getBundle("org.eclipse.jdt.junit6.runtime"), "org.eclipse.jdt.junit6.runtime bundle missing"); //$NON-NLS-1$ //$NON-NLS-2$

		JUnitExecutionTest.setupProjects();
	}
	public static Object[][] parameters() {
		return new Object[][] {
				{ "JUnit6", getJProject("verification.tests.junit6.suite"), "verification.tests.junit6" },
				{ "JUnit5", getJProject("verification.tests.junit5.suite"), "verification.tests.junit5" },
		};
	}
// Just for display

@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	public void executeSuite(String testCaseName, IJavaProject project, String packageName) throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(findType(project, "TestSuite"));
		JUnitExecutionTest.assertSuccessful(session);
		String expected = String.format("""
				%1$s.suite.TestSuite
				  JUnit Jupiter
				    %1$s.Test1
				      test1(%1$s.Test1)
				      test2(%1$s.Test1)
				    %1$s.Test2
				      test(%1$s.Test2)
				""", packageName);
		Assertions.assertEquals(expected.strip(), toString(session).strip());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	public void executePackage(String testCaseName, IJavaProject project, String packageName) throws Exception {
		ITestRunSession session = TestExecutionUtil.runTest(findType(project, "TestSuite").getPackageFragment());
		JUnitExecutionTest.assertSuccessful(session);
		assertThat(session.getChildren()).isNotEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	public void executeProject(String testCaseName, IJavaProject project, String packageName) throws Exception {
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
