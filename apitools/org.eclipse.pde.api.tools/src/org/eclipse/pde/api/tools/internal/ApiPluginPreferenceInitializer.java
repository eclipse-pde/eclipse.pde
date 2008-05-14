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
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Initializes all of the core preferences for the {@link ApiPlugin}
 * 
 * @since 1.0.0
 */
public class ApiPluginPreferenceInitializer extends AbstractPreferenceInitializer {
	/**
	 * Constructor
	 */
	public ApiPluginPreferenceInitializer() {
	}

	public void initializeDefaultPreferences() {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		
		// usage
		prefs.setDefault(IApiProblemTypes.ILLEGAL_EXTEND, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_INSTANTIATE, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_REFERENCE, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.ILLEGAL_OVERRIDE, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.LEAK_EXTEND, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.LEAK_FIELD_DECL, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.LEAK_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.LEAK_METHOD_PARAM, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, ApiPlugin.VALUE_WARNING);
		prefs.setDefault(IApiProblemTypes.INVALID_JAVADOC_TAG, ApiPlugin.VALUE_IGNORE);
		
		// compatibilities
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			prefs.setDefault(ApiPlugin.AllCompatibilityKeys[i], ApiPlugin.VALUE_ERROR);
		}
	
		// version management
		prefs.setDefault(IApiProblemTypes.MISSING_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.MALFORMED_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
		prefs.setDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, ApiPlugin.VALUE_DISABLED);
		prefs.setDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, ApiPlugin.VALUE_DISABLED);
		
		prefs.setDefault(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, ApiPlugin.VALUE_WARNING);
	}

}
