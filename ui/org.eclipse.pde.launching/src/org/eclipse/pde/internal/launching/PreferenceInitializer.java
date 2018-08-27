/*******************************************************************************
 * Copyright (c) 2009, 2015 eXXcellent solutions gmbh, IBM Corporation, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import org.eclipse.core.runtime.preferences.*;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(IPDEConstants.PLUGIN_ID);
		prefs.put(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, OSGiFrameworkManager.DEFAULT_FRAMEWORK);
		prefs.put(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION, "${workspace_loc}/../runtime-"); //$NON-NLS-1$
		prefs.putBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER, true);
		prefs.put(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION, "${workspace_loc}/../junit-workspace"); //$NON-NLS-1$
		prefs.putBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER, false);

		// copy over instance scope prefs from UI plugin
		IEclipsePreferences oldInstancePrefs = InstanceScope.INSTANCE.getNode(IPDEConstants.UI_PLUGIN_ID);
		IEclipsePreferences newInstancePrefs = InstanceScope.INSTANCE.getNode(IPDEConstants.PLUGIN_ID);

		String osgiFramework = oldInstancePrefs.get(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, null);
		if (osgiFramework != null) {
			newInstancePrefs.put(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, osgiFramework);
			oldInstancePrefs.remove(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
		}

		String autoManage = oldInstancePrefs.get(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, null);
		if (autoManage != null) {
			newInstancePrefs.put(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, autoManage);
			oldInstancePrefs.remove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE);
		}

		try {
			newInstancePrefs.flush();
			oldInstancePrefs.flush();
		} catch (BackingStoreException e) {
			PDELaunchingPlugin.log(e);
		}
	}
}
