/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.osgi.service.prefs.BackingStoreException;

public final class PDEPreferencesManager {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private IEclipsePreferences fDefaultScopePrefs;
	private IEclipsePreferences fInstanceScopePrefs;

	public PDEPreferencesManager(String ID) {
		fInstanceScopePrefs = new InstanceScope().getNode(ID);
		fDefaultScopePrefs = new DefaultScope().getNode(ID);
	}

	public boolean getBoolean(String key) {
		return fInstanceScopePrefs.getBoolean(key, fDefaultScopePrefs.getBoolean(key, false));
	}

	public int getInt(String key) {
		return fInstanceScopePrefs.getInt(key, fDefaultScopePrefs.getInt(key, 0));
	}

	public String getDefaultString(String key) {
		return fDefaultScopePrefs.get(key, EMPTY_STRING);
	}

	public boolean getDefaultBoolean(String key) {
		return fDefaultScopePrefs.getBoolean(key, false);
	}

	public int getDefaultInt(String key) {
		return fDefaultScopePrefs.getInt(key, 0);
	}

	public String getString(String key) {
		return fInstanceScopePrefs.get(key, fDefaultScopePrefs.get(key, EMPTY_STRING));
	}

	public void savePluginPreferences() {
		try {
			fInstanceScopePrefs.flush();
		} catch (BackingStoreException e) {
			PDECore.logException(e);
		}
	}

	public void setDefault(String key, String value) {
		fDefaultScopePrefs.put(key, value);
	}

	public void setDefault(String key, boolean value) {
		fDefaultScopePrefs.putBoolean(key, value);
	}

	public void setDefault(String key, int value) {
		fDefaultScopePrefs.putInt(key, value);
	}

	public void setToDefault(String key) {
		fInstanceScopePrefs.put(key, fDefaultScopePrefs.get(key, EMPTY_STRING));
	}

	public void setValue(String key, int value) {
		fInstanceScopePrefs.putInt(key, value);
	}

	public void setValue(String key, boolean value) {
		fInstanceScopePrefs.putBoolean(key, value);
	}

	public void setValue(String key, String value) {
		fInstanceScopePrefs.put(key, value);
	}

	public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
		fInstanceScopePrefs.removePreferenceChangeListener(listener);
	}

	public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
		fInstanceScopePrefs.addPreferenceChangeListener(listener);
	}
}
