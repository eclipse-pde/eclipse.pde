/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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

/**
 * Provides old {@link org.eclipse.core.runtime.Preferences} like interface to preferences but uses {@link IEclipsePreferences} instead
 *
 */
public final class PDEPreferencesManager {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private IEclipsePreferences fDefaultScopePrefs;
	private IEclipsePreferences fInstanceScopePrefs;

	/**
	 * Creates the preferences manager for the scope defined by ID
	 * @param ID scope for the preferences
	 */
	public PDEPreferencesManager(String ID) {
		fInstanceScopePrefs = new InstanceScope().getNode(ID);
		fDefaultScopePrefs = new DefaultScope().getNode(ID);
	}

	/**
	 * Register the given listener for notification of preference changes.
	 * Calling this method multiple times with the same listener has no effect. The
	 * given listener argument must not be <code>null</code>.
	 * 
	 * @param listener the preference change listener to register
	 * @see #removePreferenceChangeListener(IEclipsePreferences.IPreferenceChangeListener)
	 * @see IEclipsePreferences.IPreferenceChangeListener
	 */
	public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
		fInstanceScopePrefs.addPreferenceChangeListener(listener);
	}

	/**
	 * Returns the <code>boolean</code> value  associated with the specified <code>key</code> 
	 * in the instance scope. 
	 * 
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the instance scope, the backing store is inaccessible, or if the associated
	 * value is something other than "true" or "false", ignoring case. Use {@link #setValue(String, boolean)} 
	 * to set the value of this preference key.
	 * </p>
	 * 
	 * @param key <code>key</code> whose associated value is to be returned as a
	 *        <code>boolean</code>.
	 * @return the <code>boolean</code> value associated with <code>key</code>, or
	 *         <code>null</code> if the associated value does not exist in either scope or cannot
	 *         be interpreted as a <code>boolean</code>.
	 * @see #setValue(String, boolean)
	 * @see #setValueOrRemove(String, boolean)
	 * @see #setDefault(String, boolean)
	 */
	public boolean getBoolean(String key) {
		return fInstanceScopePrefs.getBoolean(key, fDefaultScopePrefs.getBoolean(key, false));
	}

	/**
	 * Returns the <code>boolean</code> value associated with the specified <code>key</code> 
	 * in the default scope. 
	 * 
	 * <p>
	 * Returns <code>false</code> if there is no value associated with the
	 * <code>key</code> in the default scope, the backing store is inaccessible, or if the associated
	 * value is something other than "true" or "false", ignoring case. Use {@link #setDefault(String, boolean)} 
	 * to set the default value of this preference key.
	 * </p>
	 * 
	 * @param key <code>key</code> whose associated value is to be returned as a
	 *        <code>boolean</code>.
	 * @return the <code>boolean</code> value associated with <code>key</code>, or
	 *         <code>false</code> if the associated value does not exist in default scope or cannot
	 *         be interpreted as a <code>boolean</code>.
	 * @see #setDefault(String, boolean)
	 */
	public boolean getDefaultBoolean(String key) {
		return fDefaultScopePrefs.getBoolean(key, false);
	}

	/**
	 * Returns the <code>int</code> value associated with the specified <code>key</code> 
	 * in the default scope. 
	 * 
	 * <p>
	 * Returns <code>0</code> if there is no value associated with the
	 * <code>key</code> in the default scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value. Use {@link #setDefault(String, int)} 
	 * to set the default value of this preference key.
	 * </p>
	 * 
	 * @param key <code>key</code> whose associated value is to be returned as a
	 *        <code>int</code>.
	 * @return the <code>int</code> value associated with <code>key</code>, or
	 *         <code>0</code> if the associated value does not exist in default scope or cannot
	 *         be interpreted as a <code>int</code>.
	 * @see #setDefault(String, int)
	 */
	public int getDefaultInt(String key) {
		return fDefaultScopePrefs.getInt(key, 0);
	}

	/**
	 * Returns the value associated with the specified <code>key</code> 
	 * in the default scope. 
	 * 
	 * <p>
	 * Returns empty string <code>""</code> if there is no value associated with the
	 * <code>key</code> in the default scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value. Use {@link #setDefault(String, String)} 
	 * to set the default value of this preference key.
	 * </p>
	 * 
	 * @param key <code>key</code> whose associated value is to be returned.
	 * @return the value associated with <code>key</code>, or
	 *         empty string <code>""</code> if the associated value does not exist in default scope.
	 * @see #setDefault(String, String)
	 */
	public String getDefaultString(String key) {
		return fDefaultScopePrefs.get(key, EMPTY_STRING);
	}

	/**
	 * Returns the <code>int</code> value associated with the specified <code>key</code> 
	 * in the instance scope. 
	 * 
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the instance scope, the backing store is inaccessible, or if the associated
	 * value is something that can not be parsed as an integer value. Use {@link #setValue(String, int)} 
	 * to set the value of this preference key.
	 * </p>
	 * 
	 * @param key key whose associated value is to be returned as an <code>int</code>.
	 * @return the <code>int</code> value associated with <code>key</code>, or
	 *         <code>0</code> if the associated value does not exist in either scope or cannot
	 *         be interpreted as an <code>int</code>.
	 * @see #setValue(String, int)
	 * @see #setValueOrRemove(String, int)
	 * @see #setDefault(String, int)
	 */
	public int getInt(String key) {
		return fInstanceScopePrefs.getInt(key, fDefaultScopePrefs.getInt(key, 0));
	}

	/**
	 * Returns the value associated with the specified <code>key</code> in the instance scope. 
	 * 
	 * <p>
	 * Returns the value specified in the default scope if there is no value associated with the
	 * <code>key</code> in the instance scope, the backing store is inaccessible. Use {@link #setValue(String, String)} 
	 * to set the value of this preference key.
	 * </p>
	 * 
	 * @param key key whose associated value is to be returned.
	 * @return the value associated with <code>key</code>, or
	 *         <code>null</code> if the associated value does not exist in either scope.
	 * @see #setValue(String, String)
	 * @see #setValueOrRemove(String, String)
	 * @see #setDefault(String, String)
	 */
	public String getString(String key) {
		return fInstanceScopePrefs.get(key, fDefaultScopePrefs.get(key, EMPTY_STRING));
	}

	/**
	 * De-register the given listener from receiving notification of preference changes
	 * Calling this method multiple times with the same listener has no
	 * effect. The given listener argument must not be <code>null</code>.
	 * 
	 * @param listener the preference change listener to remove
	 * @see #addPreferenceChangeListener(IEclipsePreferences.IPreferenceChangeListener)
	 * @see IEclipsePreferences.IPreferenceChangeListener
	 */
	public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
		fInstanceScopePrefs.removePreferenceChangeListener(listener);
	}

	/**
	 * Forces any changes in the preferences to the persistent store.
	 */
	public void savePluginPreferences() {
		try {
			fInstanceScopePrefs.flush();
		} catch (BackingStoreException e) {
			PDECore.logException(e);
		}
	}

	/**
	 * Associates the specified <code>boolean</code> value with the specified key in the default scope.
	 * This method is intended for use in conjunction with the {@link #getDefaultBoolean(String)} method.
	 * 
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getDefaultBoolean(String)
	 * @see #getBoolean(String)
	 */
	public void setDefault(String key, boolean value) {
		fDefaultScopePrefs.putBoolean(key, value);
	}

	/**
	 * Associates the specified <code>int</code> value with the specified key in the default scope.
	 * This method is intended for use in conjunction with the {@link #getDefaultInt(String)} method.
	 * 
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getDefaultInt(String)
	 * @see #getInt(String)
	 */
	public void setDefault(String key, int value) {
		fDefaultScopePrefs.putInt(key, value);
	}

	/**
	 * Associates the specified <code>String</code> value with the specified key in the default scope.
	 * This method is intended for use in conjunction with the {@link #getDefaultString(String)} method.
	 * 
	 * @param key <code>key</code> with which the string form of value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getDefaultString(String)
	 * @see #getString(String)
	 */
	public void setDefault(String key, String value) {
		fDefaultScopePrefs.put(key, value);
	}

	/**
	 * Sets the current value of the preference with the given name back to its default value. 
	 * The given name must not be <code>null</code>.
	 *
	 * @param key the name of the preference
	 */
	public void setToDefault(String key) {
		fInstanceScopePrefs.put(key, fDefaultScopePrefs.get(key, EMPTY_STRING));
	}

	/**
	 * Associates the specified <code>boolean</code> value with the specified key in the instance scope.
	 * This method is intended for use in conjunction with the {@link #getBoolean(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>boolean</code> value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getBoolean(String)
	 * @see #getDefaultBoolean(String)
	 * @see #setToDefault(String)
	 */
	public void setValue(String key, boolean value) {
		fInstanceScopePrefs.putBoolean(key, value);
	}

	/**
	 * Associates the specified <code>int</code> value with the specified key in the instance scope.
	 * This method is intended for use in conjunction with the {@link #getInt(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>int</code> value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getInt(String)
	 * @see #getDefaultInt(String)
	 * @see #setToDefault(String)
	 */
	public void setValue(String key, int value) {
		fInstanceScopePrefs.putInt(key, value);
	}

	/**
	 * Associates the specified <code>String</code> value with the specified key in the instance scope.
	 * This method is intended for use in conjunction with the {@link #getString(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>String</code> value is to be associated.
	 * @param value <code>value</code> to be associated with <code>key</code>.
	 * @see #getString(String)
	 * @see #getDefaultString(String)
	 * @see #setToDefault(String)
	 */
	public void setValue(String key, String value) {
		fInstanceScopePrefs.put(key, value);
	}

	/**
	 * Associates the specified <code>boolean</code> value with the specified key in the instance scope
	 * or removes the preference if the value is equal to the value in the default scope.
	 * <p>
	 * This method is intended for use in conjunction with the {@link #getBoolean(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>boolean</code> value is to be associated
	 * @param value <code>value</code> to be associated with <code>key</code>
	 * @see #getBoolean(String)
	 * @see #getDefaultBoolean(String)
	 * @see #setToDefault(String)
	 */
	public void setValueOrRemove(String key, boolean value) {
		if (value == getDefaultBoolean(key)) {
			fInstanceScopePrefs.remove(key);
		} else {
			fInstanceScopePrefs.putBoolean(key, value);
		}
	}

	/**
	 * Associates the specified <code>int</code> value with the specified key in the instance scope
	 * or removes the preference if the value is equal to the value in the default scope.
	 * <p>
	 * This method is intended for use in conjunction with the {@link #getInt(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>int</code> value is to be associated
	 * @param value <code>value</code> to be associated with <code>key</code>
	 * @see #getInt(String)
	 * @see #getDefaultInt(String)
	 * @see #setToDefault(String)
	 */
	public void setValueOrRemove(String key, int value) {
		if (value == getDefaultInt(key)) {
			fInstanceScopePrefs.remove(key);
		} else {
			fInstanceScopePrefs.putInt(key, value);
		}
	}

	/**
	 * Associates the specified <code>String</code> value with the specified key in the instance scope
	 * or removes the preference if the value is equal to the value in the default scope.
	 * <p>
	 * This method is intended for use in conjunction with the {@link #getString(String)} method.
	 * 
	 * @param key <code>key</code> with which the <code>String</code> value is to be associated
	 * @param value <code>value</code> to be associated with <code>key</code>
	 * @see #getString(String)
	 * @see #getDefaultString(String)
	 * @see #setToDefault(String)
	 */
	public void setValueOrRemove(String key, String value) {
		if (value.equals(getDefaultString(key))) {
			fInstanceScopePrefs.remove(key);
		} else {
			fInstanceScopePrefs.put(key, value);
		}
	}

	/**
	 * Forces any changes in the contents of the instance node and its descendants to
	 * the persistent store.
	 * 
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @see org.osgi.service.prefs.Preferences#flush()
	 */
	public void flush() throws BackingStoreException {
		fInstanceScopePrefs.flush();
	}
}
