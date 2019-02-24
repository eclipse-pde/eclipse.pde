/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		//set defaults for compiler preferences in org.eclipse.pde pref node, not org.eclipse.pde.core
		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(PDE.PLUGIN_ID);
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
		prefs.putInt(CompilerFlags.P_SERVICE_COMP_WITHOUT_LAZY_ACT, CompilerFlags.WARNING);
		prefs.putInt(CompilerFlags.P_NO_AUTOMATIC_MODULE, CompilerFlags.WARNING);

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
		prefs.putInt(CompilerFlags.P_MISSING_EXPORT_PKGS, CompilerFlags.WARNING);

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

		// Now init pde.core preferences
		PDEPreferencesManager corePrefs = PDECore.getDefault().getPreferencesManager();
		corePrefs.setDefault(ICoreConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET, true);
		corePrefs.setDefault(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, false);
		corePrefs.setDefault(ICoreConstants.TEST_PLUGIN_PATTERN, ICoreConstants.TEST_PLUGIN_PATTERN_DEFAULTVALUE);
	}
}
