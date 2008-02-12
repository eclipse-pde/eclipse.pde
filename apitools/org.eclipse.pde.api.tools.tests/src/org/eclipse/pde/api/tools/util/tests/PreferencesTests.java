/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tests for the severity preferences
 * @since
 */
public class PreferencesTests extends AbstractApiTest {

	/**
	 * Sets up a variety of preferences, including adding project specific preferences 
	 * to the test project
	 */
	public void testSetupSettings() {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		prefs.setValue(ApiPlugin.RESTRICTION_NOINSTANTIATE, ApiPlugin.VALUE_ERROR);
		ApiPlugin.getDefault().savePluginPreferences();
		assertTrue("The plugin setting should have been saved", !prefs.needsSaving());
		
		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		ProjectScope scope = new ProjectScope(project.getProject());
		IEclipsePreferences eprefs = scope.getNode(ApiPlugin.getPluginIdentifier());
		assertNotNull("The ApiPlugin section for project settings should be available", eprefs);
		eprefs.put(ApiPlugin.RESTRICTION_NOREFERENCE, ApiPlugin.VALUE_IGNORE);
		try {
			eprefs.flush();
		} catch (BackingStoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * tests that the default preferences are set of the ApiPlugin
	 */
	public void testGetDefaultSeverity() {
		String value = ApiPlugin.getDefault().getPluginPreferences().getDefaultString(ApiPlugin.RESTRICTION_NOEXTEND);
		assertEquals("The default value for RESTRICTION_NOEXTEND should be 'Warning'", value, ApiPlugin.VALUE_WARNING);
	}
	
	/**
	 * Tests getting a default value from the getSeverityLevel() method is correct
	 */
	public void testGetSeverityReturnsDefault() {
		int value = ApiPlugin.getDefault().getSeverityLevel(ApiPlugin.RESTRICTION_NOIMPLEMENT, null);
		assertEquals("The default value for RESTRICTION_NOIMPLEMENT should be 'Warning'", value, ApiPlugin.SEVERITY_WARNING);
	}
	
	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct
	 */
	public void testGetNonDefaultValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(ApiPlugin.RESTRICTION_NOINSTANTIATE, null);
		assertEquals("The value for RESTRICTION_NOINSTANTIATE should be 'Error'", value, ApiPlugin.SEVERITY_ERROR);
	}
	
	/**
	 * Tests that getting a set value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	public void testGetProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(ApiPlugin.RESTRICTION_NOREFERENCE, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOREFERENCE should be 'Ignore'", value, ApiPlugin.SEVERITY_IGNORE);
	}
	
	/**
	 * Tests that getting a default value the getSeverityLevel() method is correct for project
	 * specific settings
	 */
	public void testGetDefaultProjectSpecificValue() {
		int value = ApiPlugin.getDefault().getSeverityLevel(ApiPlugin.RESTRICTION_NOEXTEND, getTestingJavaProject(TESTING_PROJECT_NAME).getProject());
		assertEquals("The value for RESTRICTION_NOEXTEND should be 'Warning'", value, ApiPlugin.SEVERITY_WARNING);
	}
	
}
