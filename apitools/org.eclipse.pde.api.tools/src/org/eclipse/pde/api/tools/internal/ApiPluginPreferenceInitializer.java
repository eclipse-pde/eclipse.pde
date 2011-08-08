/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
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
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		if(node == null) {
			return;
		}
		
		// usage
		node.put(IApiProblemTypes.ILLEGAL_EXTEND, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.ILLEGAL_INSTANTIATE, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.ILLEGAL_REFERENCE, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.ILLEGAL_OVERRIDE, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.LEAK_EXTEND, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.LEAK_FIELD_DECL, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.LEAK_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.LEAK_METHOD_PARAM, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.INVALID_JAVADOC_TAG, ApiPlugin.VALUE_IGNORE);
		node.put(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, ApiPlugin.VALUE_IGNORE);
		node.put(IApiProblemTypes.UNUSED_PROBLEM_FILTERS, ApiPlugin.VALUE_WARNING);
		node.put(IApiProblemTypes.FATAL_PROBLEMS, ApiPlugin.VALUE_ERROR);
		
		// compatibilities
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			node.put(ApiPlugin.AllCompatibilityKeys[i], ApiPlugin.VALUE_ERROR);
		}
		node.put(IApiProblemTypes.REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED, ApiPlugin.VALUE_DISABLED);

		// version management
		node.put(IApiProblemTypes.MISSING_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.MALFORMED_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, ApiPlugin.VALUE_DISABLED);
		node.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, ApiPlugin.VALUE_DISABLED);
		
		node.put(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.MISSING_EE_DESCRIPTIONS, ApiPlugin.VALUE_ERROR);

		// api component resolution
		node.put(IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT, ApiPlugin.VALUE_WARNING);
		node.putBoolean(IApiProblemTypes.AUTOMATICALLY_REMOVE_UNUSED_PROBLEM_FILTERS, false);
		
		// api use scans
		node.put(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY, ApiPlugin.VALUE_ERROR);
		node.put(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY, ApiPlugin.VALUE_ERROR);
	}

}
