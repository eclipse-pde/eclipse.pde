/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
		IEclipsePreferences defaultPreferences = new DefaultScope().getNode(PDECore.PLUGIN_ID);
		IEclipsePreferences preferences = new InstanceScope().getNode(PDECore.PLUGIN_ID);
		defaultPreferences.put(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		defaultPreferences.put(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		defaultPreferences.put(ICoreConstants.CHECKED_VERSION_PLUGINS, ICoreConstants.VALUE_SAVED_NONE);
		if (preferences.get(ICoreConstants.TARGET_MODE, defaultPreferences.get(ICoreConstants.TARGET_MODE, "")).equals(ICoreConstants.VALUE_USE_THIS)) { //$NON-NLS-1$
			preferences.put(ICoreConstants.PLATFORM_PATH, TargetPlatform.getDefaultLocation());
		} else {
			defaultPreferences.put(ICoreConstants.PLATFORM_PATH, TargetPlatform.getDefaultLocation());
		}
		// set defaults for the target environment variables.
		defaultPreferences.put(ICoreConstants.OS, Platform.getOS());
		defaultPreferences.put(ICoreConstants.WS, Platform.getWS());
		defaultPreferences.put(ICoreConstants.NL, Locale.getDefault().toString());
		defaultPreferences.put(ICoreConstants.ARCH, Platform.getOSArch());
		defaultPreferences.putBoolean(ICoreConstants.TARGET_PLATFORM_REALIZATION, TargetPlatform.getDefaultLocation().equals(TargetPlatform.getLocation()));
		try {
			preferences.flush();
			defaultPreferences.flush();
		} catch (BackingStoreException bse) {
			PDECore.log(bse);
		}
		//set defaults for compiler preferences in org.eclipse.pde pref node, not org.eclipse.pde.core
		IEclipsePreferences prefs = new DefaultScope().getNode(PDE.PLUGIN_ID);
		prefs.putInt(CompilerFlags.P_UNRESOLVED_IMPORTS, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_UNRESOLVED_EX_POINTS, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_NO_REQUIRED_ATT, CompilerFlags.ERROR);
		prefs.putInt(CompilerFlags.P_UNKNOWN_ELEMENT, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_ATTRIBUTE, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_DEPRECATED, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_INTERNAL, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_CLASS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_DISCOURAGED_CLASS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_RESOURCE, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_UNKNOWN_IDENTIFIER, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_NOT_EXTERNALIZED, CompilerFlags.IGNORE);

		prefs.putInt(CompilerFlags.P_BUILD, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_MISSING_OUTPUT, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_BUILD_SOURCE_LIBRARY, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_OUTPUT_LIBRARY, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_SRC_INCLUDES, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_BIN_INCLUDES, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_JAVA_COMPLIANCE, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_BUILD_JAVA_COMPILER, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_BUILD_ENCODINGS, CompilerFlags.IGNORE);

		prefs.putInt(CompilerFlags.P_INCOMPATIBLE_ENV, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_MISSING_EXPORT_PKGS, CompilerFlags.IGNORE);

		prefs.putInt(CompilerFlags.P_MISSING_VERSION_EXP_PKG, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_MISSING_VERSION_IMP_PKG, CompilerFlags.IGNORE);
		prefs.putInt(CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE, CompilerFlags.IGNORE);

		prefs.putBoolean(CompilerFlags.S_CREATE_DOCS, false);
		prefs.put(CompilerFlags.S_DOC_FOLDER, "doc"); //$NON-NLS-1$
		prefs.putInt(CompilerFlags.S_OPEN_TAGS, CompilerFlags.WARNING);

		prefs.putInt(CompilerFlags.F_UNRESOLVED_PLUGINS, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.F_UNRESOLVED_FEATURES, CompilerFlags.WARNING);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			PDECore.log(e);
		}
	}
}
