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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemTypes;

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
		prefs.setDefault(IApiProblemTypes.ILLEGAL_EXTEND, warnings[0]);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_IMPLEMENT, warnings[0]);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_INSTANTIATE, warnings[0]);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_REFERENCE, warnings[0]);
		
		//binary compatibilities
		for (int i = 0, max = ApiPlugin.AllBinaryCompatibilityKeys.length; i < max; i++) {
			prefs.setDefault(ApiPlugin.AllBinaryCompatibilityKeys[i], warnings[1]);
		}

		// version management
		prefs.setDefault(IApiProblemTypes.MISSING_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.MALFORMED_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
	}

}
