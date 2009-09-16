/*******************************************************************************
 * Copyright (c) 2009 eXXcellent solutions gmbh and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;


import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;

import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = new DefaultScope().getNode(IPDEConstants.PLUGIN_ID);
		prefs.put(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK, OSGiFrameworkManager.DEFAULT_FRAMEWORK);

		// copy over instance scope prefs from UI plugin
		IEclipsePreferences oldInstancePrefs = new InstanceScope().getNode(IPDEConstants.UI_PLUGIN_ID);
		IEclipsePreferences newInstancePrefs = new InstanceScope().getNode(IPDEConstants.PLUGIN_ID);

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
