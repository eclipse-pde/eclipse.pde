/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.jupiter.api.Assertions;

/**
 * Tests usage scanning in source
 *
 * @since 1.0.0
 */
public abstract class UsageTest extends ApiBuilderTest {

	private static final String USAGE = "usage"; //$NON-NLS-1$
	protected static final String TESTING_PACKAGE = "x.y.z"; //$NON-NLS-1$
	protected static final String REPLACEMENT_PACKAGE = "x.y.z.replace"; //$NON-NLS-1$
	protected static final String REF_PROJECT_NAME = "refproject"; //$NON-NLS-1$
	protected static final String TESTING_PROJECT = "usagetests"; //$NON-NLS-1$
	protected static final String INNER_NAME1 = "inner"; //$NON-NLS-1$
	protected static final String OUTER_NAME = "outer"; //$NON-NLS-1$
	protected static final String INNER_NAME2 = "inner2"; //$NON-NLS-1$
	protected static final String OUTER_INAME = "Iouter"; //$NON-NLS-1$

	public static IPath SOURCE_PATH = IPath.fromOSString("src/x/y/z"); //$NON-NLS-1$

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(true);
		enableVersionNumberOptions(false);
	}

	/**
	 * Makes sure the compliance for the project is what the test says it should
	 * be
	 */
	protected void ensureCompliance(String[] projectnames) {
		IJavaProject project = null;
		String compliance = null;
		for (String projectname : projectnames) {
			project = getEnv().getJavaProject(projectname);
			compliance = getTestCompliance();
			if (!compliance.equals(project.getOption(JavaCore.COMPILER_COMPLIANCE, true))) {
				getEnv().setProjectCompliance(project, compliance);
			}
		}
	}

	@Override
	protected IPath getTestSourcePath() {
		return IPath.fromOSString(USAGE);
	}

	@Override
	protected String getTestingProjectName() {
		return "usagetests"; //$NON-NLS-1$
	}

	/**
	 * Deploys a standard API usage test with the test project being created and
	 * the given source is imported in the testing project into the given
	 * project.
	 *
	 * This method assumes that the reference and testing project have been
	 * imported into the workspace already.
	 *
	 * @param inc if an incremental build should be done
	 */
	protected void deployUsageTest(String typename, boolean inc) {
		try {
			IPath typepath = IPath.fromOSString(getTestingProjectName()).append(SOURCE_PATH).append(typename).addFileExtension("java"); //$NON-NLS-1$
			createWorkspaceFile(typepath, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(typename).addFileExtension("java")); //$NON-NLS-1$
			if (inc) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			expectingNoJDTProblemsFor(typepath);
			ApiProblem[] problems = getEnv().getProblemsFor(typepath, null);
			assertProblems(problems);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		indexDisabledForTest = false;
		// If we have an existing environment, set it to revert rather than
		// delete the workspace to improve performance
		resetBuilderOptions();
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
			env.setRevertSourcePath(null);
		}
		super.setUp();

		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		if (!project.exists()) {
			// populate the workspace with initial plug-ins/projects
			createExistingProjects("usageprojects", true, true, false); //$NON-NLS-1$
		}
		ensureCompliance(new String[] { getTestingProjectName() });
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(false);
		}
	}

	}
