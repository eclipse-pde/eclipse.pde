/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import org.eclipse.osgi.util.NLS;

public class PDERuntimeMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.runtime.pderuntimeresources";//$NON-NLS-1$

	public static String RegistryView_refresh_label;
	public static String RegistryView_refresh_tooltip;
	public static String RegistryView_collapseAll_label;
	public static String RegistryView_collapseAll_tooltip;
	public static String RegistryView_folders_imports;
	public static String RegistryView_folders_libraries;
	public static String RegistryView_folders_extensionPoints;
	public static String RegistryView_folders_extensions;

	public static String RegistryView_found_problems;
	public static String RegistryView_showRunning_label;
	public static String RegistryView_showDisabled_label;
	public static String RegistryView_showAdvanced_label;
	public static String RegistryView_titleSummary;
	public static String RegistryView_startAction_label;
	public static String RegistryView_stopAction_label;
	public static String RegistryView_enableAction_label;
	public static String RegistryView_diag_dialog_title;

	public static String RegistryView_diagnoseAction_label;
	public static String RegistryView_disableAction_label;
	public static String RegistryView_no_unresolved_constraints;

	public static String MessageHelper_missing_optional_required_bundle;
	public static String MessageHelper_missing_required_bundle;
	public static String MessageHelper_missing_imported_package;
	public static String MessageHelper_missing_host;

	public static String RegistryBrowserLabelProvider_nameIdBind;

	public static String SpyDialog_title;
	public static String SpyDialog_close;
	public static String SpyDialog_activeShell_title;
	public static String SpyDialog_activeShell_desc;
	public static String SpyDialog_activePart_title;
	public static String SpyDialog_activePart_desc;
	public static String SpyDialog_activeWizard_title;
	public static String SpyDialog_activeWizard_desc;
	public static String SpyDialog_activeMenuIds;
	public static String SpyDialog_contributingPluginId_title;
	public static String SpyDialog_contributingPluginId_desc;
	public static String SpyDialog_activeSelection_title;
	public static String SpyDialog_activeSelection_desc;
	public static String SpyDialog_activeSelectionInterfaces_desc;
	public static String SpyDialog_activeDialogPageSection_title;
	public static String SpyDialog_activeDialogPageSection_title2;
	public static String SpyDialog_activeDialogPageSection_desc;
	public static String SpyDialog_activeHelpSection_title;
	public static String SpyDialog_activeHelpSection_desc;

	public static String SpyFormToolkit_saveImageAs_title;
	public static String SpyFormToolkit_copyQualifiedName;

	public static String RegistryBrowser_copy_label;
	public static String RegistryBrowser_showExtOnlyLabel;
	public static String RegistryBrowser_plugins;

	public static String RegistryBrowserLabelProvider_contributedBy;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDERuntimeMessages.class);
	}

}
