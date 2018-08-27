/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the severity preferences
 * @since
 */
public class PreferencesTests extends AbstractApiTest {

	@Before
	public void setUp() throws Exception {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The instance node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.ILLEGAL_INSTANTIATE, ApiPlugin.VALUE_ERROR);
		inode.flush();

		createProject(TESTING_PROJECT_NAME, null);

		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		assertNotNull("the testing project must not be null", project); //$NON-NLS-1$
		ProjectScope scope = new ProjectScope(project.getProject());
		IEclipsePreferences eprefs = scope.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The ApiPlugin section for project settings should be available", eprefs); //$NON-NLS-1$
		eprefs.put(IApiProblemTypes.ILLEGAL_REFERENCE, ApiPlugin.VALUE_IGNORE);
		eprefs.flush();
	}

	@After
	public void tearDown() throws Exception {
		deleteProject(TESTING_PROJECT_NAME);
	}

	/**
	 * tests that the default preferences are set of the ApiPlugin
	 */
	@Test
	public void testGetDefaultSeverity() {
		IEclipsePreferences dnode = DefaultScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("the default node must exist", dnode); //$NON-NLS-1$
		String value = dnode.get(IApiProblemTypes.ILLEGAL_EXTEND, null);
		assertEquals("The default value for RESTRICTION_NOEXTEND should be 'Warning'", ApiPlugin.VALUE_WARNING, value); //$NON-NLS-1$
	}

	/**
	 * Tests getting a default value from the getSeverityLevel() method is correct
	 */
	@Test
	public void testGetSeverityReturnsDefault() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_IMPLEMENT, null);
		assertEquals("The default value for RESTRICTION_NOIMPLEMENT should be 'Warning'", ApiPlugin.SEVERITY_WARNING, value); //$NON-NLS-1$
	}

	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct
	 */
	@Test
	public void testGetNonDefaultValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_INSTANTIATE, null);
		assertEquals("The value for RESTRICTION_NOINSTANTIATE should be 'Error'", ApiPlugin.SEVERITY_ERROR, value); //$NON-NLS-1$
	}

	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	@Test
	public void testGetProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_REFERENCE, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOREFERENCE should be 'Ignore'", ApiPlugin.SEVERITY_IGNORE, value); //$NON-NLS-1$
	}

	/**
	 * Tests that getting a default value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	@Test
	public void testGetDefaultProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_EXTEND, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOEXTEND should be 'Warning'", ApiPlugin.SEVERITY_WARNING, value); //$NON-NLS-1$
	}

}
