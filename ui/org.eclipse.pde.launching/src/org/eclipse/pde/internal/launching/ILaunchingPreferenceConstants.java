/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource Corporation, IBM Corporation, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

/**
 * Listing of constants used in PDE preferences for launching.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILaunchingPreferenceConstants {

	// Main preference page
	public static final String PROP_AUTO_MANAGE = "Preferences.MainPage.automanageDependencies"; //$NON-NLS-1$
	public static final String PROP_RUNTIME_WORKSPACE_LOCATION = "Preferences.MainPage.runtimeWorkspaceLocation"; //$NON-NLS-1$
	public static final String PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER = "Preferences.MainPage.runtimeWorkspaceLocationIsContainer"; //$NON-NLS-1$
	public static final String PROP_JUNIT_WORKSPACE_LOCATION = "Preferences.MainPage.junitWorkspaceLocation"; //$NON-NLS-1$
	public static final String PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER = "Preferences.MainPage.junitWorkspaceLocationIsContainer"; //$NON-NLS-1$

	// OSGi Frameworks
	public static final String DEFAULT_OSGI_FRAMEOWRK = "Preference.default.osgi.framework"; //$NON-NLS-1$
}
