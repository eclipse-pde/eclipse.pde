/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.preferences;

import junit.framework.TestCase;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.*;


/**
 * Tests to ensure that the PDE Preferences manager, added in 3.5, is working
 * correctly and is compatible with the existing preference story.
 */
public class PDEPreferencesTestCase extends TestCase {

	private static final String PLUGIN_ID = "org.eclipse.pde.core";

	public PDEPreferencesTestCase(){
		initPreferences();
	}

	private static void initPreferences(){
		PDEPreferencesManager preferences = new PDEPreferencesManager(PLUGIN_ID);
		preferences.setValue("stringKey", "stringValue");
		preferences.setValue("booleanKey", true);
		preferences.setValue("intKey", 0);
		preferences.savePluginPreferences();

		preferences.setDefault("stringKey", "defaultValue");
		preferences.setDefault("booleanKey", false);
		preferences.setDefault("intKey", -1);
	}

	public void testInstanceScopePDEPreferences(){
		PDEPreferencesManager preferences = new PDEPreferencesManager(PLUGIN_ID);
		assertEquals(preferences.getString("stringKey"), "stringValue");
		assertEquals(preferences.getBoolean("booleanKey"), true);
		assertEquals(preferences.getInt("intKey"), 0);
	}

	public void testDefaultPDEPreferences(){
		PDEPreferencesManager preferences = new PDEPreferencesManager(PLUGIN_ID);
		assertEquals(preferences.getDefaultString("stringKey"), "defaultValue");
		assertEquals(preferences.getDefaultBoolean("booleanKey"), false);
		assertEquals(preferences.getDefaultInt("intKey"), -1);
	}

	public void testPreferenceChangeListener1(){
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		final String key = "stringKey";
		String originalValue = preferences.get(key, key);

		IPreferenceChangeListener listener = new IPreferenceChangeListener(){

			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				assertEquals(event.getKey(), key);
				assertEquals(event.getNewValue(), "stringValue");
			}
		};
		preferences.addPreferenceChangeListener(listener);
		preferences.put(key, "stringValue");
		preferences.removePreferenceChangeListener(listener);

		// Restore original value
		if (originalValue != key)
			preferences.put(key, originalValue);
	}

	public void testPreferenceChangeListner2(){
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		final String key = "stringKey";
		String originalValue = preferences.get(key, key);

		preferences.put(key, "oldStringValue");

		IPreferenceChangeListener listener = new IPreferenceChangeListener(){

			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				assertEquals(event.getKey(), key);
				assertEquals(event.getOldValue(), "oldStringValue");
				assertEquals(event.getNewValue(), "newStringValue");
			}
		};
		preferences.put(key, "newStringValue");
		preferences.removePreferenceChangeListener(listener);

		// Restore original value
		if (originalValue != key)
			preferences.put(key, originalValue);
	}

	public void testCompilerPreferences(){
		// Testing the compiler preferences set by PDECore in org.eclipse.pde
		PDEPreferencesManager preferences = new PDEPreferencesManager(PDE.PLUGIN_ID);
		assertEquals(preferences.getDefaultInt(CompilerFlags.P_UNRESOLVED_IMPORTS), CompilerFlags.ERROR);
		assertEquals(preferences.getDefaultInt(CompilerFlags.P_DEPRECATED), CompilerFlags.WARNING);
		assertEquals(preferences.getDefaultInt(CompilerFlags.P_MISSING_VERSION_EXP_PKG), CompilerFlags.IGNORE);
	}

	public void testCompatibilityWithPreferenceStore(){
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		PDEPreferencesManager preferencesManager = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		assertEquals(store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS),preferencesManager.getString(IPreferenceConstants.PROP_SHOW_OBJECTS));
		assertEquals(store.getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED),preferencesManager.getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED));
		assertEquals(store.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE), preferencesManager.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));
	}

}
