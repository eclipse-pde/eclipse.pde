/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Gives access to the scope preferences
 */
public class PreferenceSpyConfiguration {

	private static String bundleId = "org.eclipse.pde.spy.preferences";

	private static IEclipsePreferences preferenceStore;

	public static IEclipsePreferences getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			preferenceStore = 	InstanceScope.INSTANCE.getNode(bundleId);;
		}
		return preferenceStore;
	}

}
