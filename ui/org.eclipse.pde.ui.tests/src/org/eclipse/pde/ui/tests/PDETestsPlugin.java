/*******************************************************************************
 *  Copyright (c) 2013, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
