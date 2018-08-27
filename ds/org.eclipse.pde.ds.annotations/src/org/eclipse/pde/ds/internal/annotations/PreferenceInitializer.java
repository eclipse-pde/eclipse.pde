/*******************************************************************************
 * Copyright (c) 2016, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		defaults.putBoolean(Activator.PREF_ENABLED, false);
		defaults.put(Activator.PREF_PATH, Activator.DEFAULT_PATH);
		defaults.put(Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name());
		defaults.putBoolean(Activator.PREF_CLASSPATH, true);
		defaults.put(Activator.PREF_VALIDATION_ERROR_LEVEL, ValidationErrorLevel.error.name());
		defaults.put(Activator.PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL, ValidationErrorLevel.error.name());
	}
}
