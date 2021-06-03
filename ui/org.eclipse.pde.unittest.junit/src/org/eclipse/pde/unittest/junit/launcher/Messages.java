/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.unittest.junit.launcher;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.pde.unittest.junit.launcher.Messages";//$NON-NLS-1$

	public static String JUnitPluginLaunchConfigurationDelegate_create_source_locator_description;
	public static String JUnitPluginLaunchConfigurationDelegate_error_input_element_deosn_not_exist;
	public static String JUnitPluginLaunchConfigurationDelegate_error_no_socket;
	public static String JUnitPluginLaunchConfigurationDelegate_error_notests_kind;
	public static String JUnitPluginLaunchConfigurationDelegate_error_wrong_input;
	public static String JUnitPluginLaunchConfigurationDelegate_input_type_does_not_exist;
	public static String JUnitPluginLaunchConfigurationDelegate_verifying_attriburtes_description;
	public static String JUnitPluginLaunchConfigurationDelegate_error_notaplugin;
	public static String JUnitPluginLaunchConfigurationDelegate_error_noStartup;
	public static String JUnitPluginLaunchConfigurationDelegate_error_missingPlugin;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}
}
