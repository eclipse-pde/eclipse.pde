/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tests for the severity preferences
 * @since
 */
public class PreferencesTests extends AbstractApiTest {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The instance node must exist", inode);
		inode.put(IApiProblemTypes.ILLEGAL_INSTANTIATE, ApiPlugin.VALUE_ERROR);
		try {
			inode.flush();
		} catch (BackingStoreException e1) {
			fail(e1.getMessage());
		}
		
		createProject(TESTING_PROJECT_NAME, null);
		
		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		assertNotNull("the testing project must not be null", project);
		ProjectScope scope = new ProjectScope(project.getProject());
		IEclipsePreferences eprefs = scope.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The ApiPlugin section for project settings should be available", eprefs);
		eprefs.put(IApiProblemTypes.ILLEGAL_REFERENCE, ApiPlugin.VALUE_IGNORE);
		try {
			eprefs.flush();
		} catch (BackingStoreException e) {
			fail(e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		deleteProject(TESTING_PROJECT_NAME);
	}
	
	/**
	 * tests that the default preferences are set of the ApiPlugin
	 */
	public void testGetDefaultSeverity() {
		IEclipsePreferences dnode = new DefaultScope().getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("the default node must exist", dnode);
		String value = dnode.get(IApiProblemTypes.ILLEGAL_EXTEND, null);
		assertEquals("The default value for RESTRICTION_NOEXTEND should be 'Warning'", ApiPlugin.VALUE_WARNING, value);
	}
	
	/**
	 * Tests getting a default value from the getSeverityLevel() method is correct
	 */
	public void testGetSeverityReturnsDefault() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_IMPLEMENT, null);
		assertEquals("The default value for RESTRICTION_NOIMPLEMENT should be 'Warning'", ApiPlugin.SEVERITY_WARNING, value);
	}
	
	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct
	 */
	public void testGetNonDefaultValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_INSTANTIATE, null);
		assertEquals("The value for RESTRICTION_NOINSTANTIATE should be 'Error'", ApiPlugin.SEVERITY_ERROR, value);
	}
	
	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	public void testGetProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_REFERENCE, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOREFERENCE should be 'Ignore'", ApiPlugin.SEVERITY_IGNORE, value);
	}
	
	/**
	 * Tests that getting a default value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	public void testGetDefaultProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_EXTEND, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOEXTEND should be 'Warning'", ApiPlugin.SEVERITY_WARNING, value);
	}
	
}
