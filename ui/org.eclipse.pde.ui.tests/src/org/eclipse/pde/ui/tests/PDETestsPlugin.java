/*******************************************************************************
 *  Copyright (c) 2013, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class PDETestsPlugin extends AbstractUIPlugin {

	private static PDETestsPlugin fgDefault = null;

	public PDETestsPlugin() {
		fgDefault = this;
	}

	/**
	 * Returns the test plug-in.
	 *
	 * @return the test plug-in
	 */
	public static PDETestsPlugin getDefault() {
		return fgDefault;
	}

	public static BundleContext getBundleContext() {
		return getDefault().getBundle().getBundleContext();
	}

}
