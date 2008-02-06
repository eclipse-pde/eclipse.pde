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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiPreferenceConstants;

/**
 * Initializes all of the core preferences for the {@link ApiPlugin}
 * 
 * @since 1.0.0
 */
public class ApiPluginPreferenceInitializer extends AbstractPreferenceInitializer {

	private String[] warnings = {ApiPlugin.VALUE_WARNING, ApiPlugin.VALUE_ERROR, ApiPlugin.VALUE_IGNORE};
	
	/**
	 * Constructor
	 */
	public ApiPluginPreferenceInitializer() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		
		//restrictions
		prefs.setDefault(ApiPlugin.RESTRICTION_NOEXTEND, warnings[0]);
		prefs.setDefault(ApiPlugin.RESTRICTION_NOIMPLEMENT, warnings[0]);
		prefs.setDefault(ApiPlugin.RESTRICTION_NOINSTANTIATE, warnings[0]);
		prefs.setDefault(ApiPlugin.RESTRICTION_NOREFERENCE, warnings[0]);
		
		//binary compatibilities
		for (int i = 0, max = ApiPlugin.AllBinaryCompatibilityKeys.length; i < max; i++) {
			prefs.setDefault(ApiPlugin.AllBinaryCompatibilityKeys[i], warnings[1]);
		}

		// version management
		prefs.setDefault(IApiPreferenceConstants.REPORT_MISSING_SINCE_TAGS, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiPreferenceConstants.REPORT_MALFORMED_SINCE_TAGS, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiPreferenceConstants.REPORT_INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiPreferenceConstants.REPORT_INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
	}

}
