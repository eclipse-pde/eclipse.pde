/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		ColorManager.initializeDefaults(store);
		store.setDefault(IPreferenceConstants.PROP_SHOW_OBJECTS, IPreferenceConstants.VALUE_USE_IDS);
		store.setDefault(IPreferenceConstants.EDITOR_FOLDING_ENABLED, false);
		store.setDefault(IPreferenceConstants.SHOW_TARGET_STATUS, false);
		store.setDefault(IPreferenceConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET, true);
		store.setDefault(IPreferenceConstants.DISABLE_API_ANALYSIS_BUILDER, false);
		store.setDefault(IPreferenceConstants.TEST_PLUGIN_PATTERN, ICoreConstants.TEST_PLUGIN_PATTERN_DEFAULTVALUE);
	}

}
