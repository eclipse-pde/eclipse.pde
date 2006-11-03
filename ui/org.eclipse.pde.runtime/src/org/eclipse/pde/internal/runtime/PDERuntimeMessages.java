/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

	public static String LogView_column_message;
	public static String LogView_column_plugin;
	public static String LogView_column_date;
	public static String LogView_clear;
	public static String LogView_clear_tooltip;
	public static String LogView_copy;
	public static String LogView_delete;
	public static String LogView_delete_tooltip;
	public static String LogView_export;
	public static String LogView_exportLog;
	public static String LogView_export_tooltip;
	public static String LogView_import;
	public static String LogView_import_tooltip;
	public static String LogView_filter;
	public static String LogView_readLog_reload;
	public static String LogView_readLog_restore;
	public static String LogView_readLog_restore_tooltip;
	public static String LogView_severity_error;
	public static String LogView_severity_warning;
	public static String LogView_severity_info;
	public static String LogView_severity_ok;
	public static String LogView_confirmDelete_title;
	public static String LogView_confirmDelete_message;
	public static String LogView_confirmOverwrite_message;
	public static String LogView_operation_importing;
	public static String LogView_operation_reloading;
	public static String LogView_activate;
	public static String LogView_view_currentLog;
	public static String LogView_view_currentLog_tooltip;
	public static String LogView_properties_tooltip;

	public static String LogView_FilterDialog_title;
	public static String LogView_FilterDialog_eventTypes;
	public static String LogView_FilterDialog_information;
	public static String LogView_FilterDialog_warning;
	public static String LogView_FilterDialog_error;
	public static String LogView_FilterDialog_limitTo;
	public static String LogView_FilterDialog_eventsLogged;
	public static String LogView_FilterDialog_allSessions;
	public static String LogView_FilterDialog_recentSession;

	public static String LogViewLabelProvider_truncatedMessage;
	
	public static String RegistryView_refresh_label;
	public static String RegistryView_refresh_tooltip;
	public static String RegistryView_collapseAll_label;
	public static String RegistryView_collapseAll_tooltip;

	public static String RegistryView_folders_imports;
	public static String RegistryView_folders_libraries;
	public static String RegistryView_folders_extensionPoints;
	public static String RegistryView_folders_extensions;
	public static String EventDetailsDialog_title;
	public static String EventDetailsDialog_date;
	public static String EventDetailsDialog_severity;
	public static String EventDetailsDialog_message;
	public static String EventDetailsDialog_exception;
	public static String EventDetailsDialog_session;
	public static String EventDetailsDialog_noStack;
	public static String EventDetailsDialog_previous;
	public static String EventDetailsDialog_next;
	public static String EventDetailsDialog_copy;

	public static String RegistryView_showRunning_label;

	public static String RegistryView_titleSummary;
	public static String OpenLogDialog_title;
	public static String OpenLogDialog_message;
	public static String OpenLogDialog_cannotDisplay;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDERuntimeMessages.class);
	}

	public static String RegistryBrowserLabelProvider_nameIdBind;
}
