/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.ui.launcher;

/**
 * A launch configuration tab that lets the user customize the list of plug-ins to launch with,
 * their start level and their auto-start attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 * @deprecated use {@link BundlesTab}
 * @noextend This class is not intended to be subclassed by clients.
 */
@Deprecated
public class EquinoxPluginsTab extends BundlesTab {

	/**
	 * Returns the default start level for the launch configuration
	 *
	 * @return the default start level
	 */
	public int getDefaultStartLevel() {
		return fFrameworkBlock.getDefaultStartLevel();
	}

}
