/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.pde.core.plugin.TargetPlatform;

public class PreferenceInitializer extends AbstractPreferenceInitializer  {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setDefault(
				ICoreConstants.TARGET_MODE,
				ICoreConstants.VALUE_USE_THIS);
		preferences.setDefault(
				ICoreConstants.CHECKED_PLUGINS,
				ICoreConstants.VALUE_SAVED_ALL);
		if (preferences.getString(ICoreConstants.TARGET_MODE).equals(ICoreConstants.VALUE_USE_THIS))
			preferences.setValue(
					ICoreConstants.PLATFORM_PATH,
					TargetPlatform.getDefaultLocation());
		else
			preferences.setDefault(
					ICoreConstants.PLATFORM_PATH,
					TargetPlatform.getDefaultLocation());

		// set defaults for the target environment variables.
		preferences.setDefault(ICoreConstants.OS, Platform.getOS());
		preferences.setDefault(ICoreConstants.WS, Platform.getWS());
		preferences.setDefault(ICoreConstants.NL, Locale.getDefault().toString());
		preferences.setDefault(ICoreConstants.ARCH, Platform.getOSArch());
	}

}