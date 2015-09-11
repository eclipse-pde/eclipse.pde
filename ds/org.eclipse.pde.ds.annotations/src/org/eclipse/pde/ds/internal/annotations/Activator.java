/*******************************************************************************
 * Copyright (c) 2012, 2015 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.pde.ds.annotations"; //$NON-NLS-1$

	public static final String PREF_ENABLED = "enabled"; //$NON-NLS-1$

	public static final String PREF_PATH = "path"; //$NON-NLS-1$

	public static final String PREF_VALIDATION_ERROR_LEVEL = "validationErrorLevel"; //$NON-NLS-1$

	public static final String PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL = "validationErrorLevel.missingImplicitUnbindMethod"; //$NON-NLS-1$

	public static final String DEFAULT_PATH = "OSGI-INF"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private DSAnnotationPreferenceListener dsPrefListener;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		dsPrefListener = new DSAnnotationPreferenceListener();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		dsPrefListener.dispose();

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
