/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

public interface ILauncherSettings {
	// Workspace data settings
	String LOCATION = "location"; //$NON-NLS-1$
	String DOCLEAR = "clearws"; //$NON-NLS-1$
	String ASKCLEAR = "askclear"; //$NON-NLS-1$
	
	// Program to run
	String APPLICATION = "application"; //$NON-NLS-1$
	String PRODUCT = "product"; //$NON-NLS-1$
	String USE_PRODUCT = "useProduct"; //$NON-NLS-1$
	String APP_TO_TEST = "testApplication"; //$NON-NLS-1$
	
	// Command line settings
	String VMINSTALL = "vminstall"; //$NON-NLS-1$
	String VMARGS = "vmargs"; //$NON-NLS-1$
	String PROGARGS = "progargs"; //$NON-NLS-1$
	String BOOTSTRAP_ENTRIES = "bootstrap"; //$NON-NLS-1$
	
	// Plug-ins and Fragments settings
	String USE_DEFAULT = "default"; //$NON-NLS-1$
	String USEFEATURES = "usefeatures"; //$NON-NLS-1$
	String WSPROJECT = "wsproject"; //$NON-NLS-1$
	String EXTPLUGINS = "extplugins"; //$NON-NLS-1$
	String INCLUDE_OPTIONAL = "includeOptional"; //$NON-NLS-1$
	String INCLUDE_FRAGMENTS = "includeFragments"; //$NON-NLS-1$
	String AUTOMATIC_ADD = "automaticAdd"; //$NON-NLS-1$
	
	// Tracing settings
	String TRACING = "tracing"; //$NON-NLS-1$
	String TRACING_OPTIONS = "tracingOptions"; //$NON-NLS-1$
	String TRACING_SELECTED_PLUGIN = "selectedPlugin"; //$NON-NLS-1$
	String TRACING_CHECKED = "checked"; //$NON-NLS-1$
	String TRACING_NONE = "[NONE]"; //$NON-NLS-1$
	
	// Configuration tab
	String CONFIG_USE_DEFAULT_AREA = "useDefaultConfigArea"; //$NON-NLS-1$
	String CONFIG_LOCATION = "configLocation"; //$NON-NLS-1$
	String CONFIG_CLEAR_AREA = "clearConfig"; //$NON-NLS-1$
	
	String CONFIG_GENERATE_DEFAULT = "useDefaultConfig"; //$NON-NLS-1$
	String CONFIG_TEMPLATE_LOCATION = "templateConfig";	 //$NON-NLS-1$
	
	// .product-specific marker
	String PRODUCT_FILE = "productFile"; //$NON-NLS-1$
			
}
