/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Locale;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setDefault(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		preferences.setDefault(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		if (preferences.getString(ICoreConstants.TARGET_MODE).equals(ICoreConstants.VALUE_USE_THIS))
			preferences.setValue(ICoreConstants.PLATFORM_PATH, TargetPlatform.getDefaultLocation());
		else
			preferences.setDefault(ICoreConstants.PLATFORM_PATH, TargetPlatform.getDefaultLocation());

		// set defaults for the target environment variables.
		preferences.setDefault(ICoreConstants.OS, Platform.getOS());
		preferences.setDefault(ICoreConstants.WS, Platform.getWS());
		preferences.setDefault(ICoreConstants.NL, Locale.getDefault().toString());
		preferences.setDefault(ICoreConstants.ARCH, Platform.getOSArch());

		preferences.setDefault(ICoreConstants.TARGET_PLATFORM_REALIZATION, TargetPlatform.getDefaultLocation().equals(TargetPlatform.getLocation()));

		//set defaults for compiler preferences in org.eclipse.pde pref node, not org.eclipse.pde.core
		IEclipsePreferences prefs = new DefaultScope().getNode(PDE.PLUGIN_ID);
		prefs.putInt(CompilerFlags.P_UNRESOLVED_IMPORTS, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_UNRESOLVED_EX_POINTS, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_NO_REQUIRED_ATT, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_UNKNOWN_ELEMENT, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_ATTRIBUTE, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_DEPRECATED, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_CLASS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_DISCOURAGED_CLASS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_RESOURCE, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_IDENTIFIER, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_NOT_EXTERNALIZED, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_BUILD, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_INCOMPATIBLE_ENV, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_MISSING_EXPORT_PKGS, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_MISSING_BUNDLE_CLASSPATH_ENTRIES, CompilerFlags.WARNING);

		prefs.putBoolean(CompilerFlags.S_CREATE_DOCS, false);
		prefs.put(CompilerFlags.S_DOC_FOLDER, "doc"); //$NON-NLS-1$
		prefs.putInt(CompilerFlags.S_OPEN_TAGS, CompilerFlags.WARNING);

		prefs.putInt(CompilerFlags.F_UNRESOLVED_PLUGINS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.F_UNRESOLVED_FEATURES, CompilerFlags.WARNING);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
	}
}
