/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
	String LOCATION = "location";
	String DOCLEAR = "clearws";
	String ASKCLEAR = "askclear";
	
	// Command line settings
	String VMINSTALL = "vminstall";
	String APPLICATION = "application";
	String APP_TO_TEST = "testApplication";
	String VMARGS = "vmargs";
	String PROGARGS = "progargs";
	String CLASSPATH_ENTRIES = "classpath";
	String SHOW_SPLASH = "showSplash";
	
	// Plug-ins and Fragments settings
	String USECUSTOM = "default";
	String USEFEATURES = "usefeatures";
	String WSPROJECT = "wsproject";
	String EXTPLUGINS = "extplugins";
	
	// Tracing settings
	String TRACING = "tracing";
	String TRACING_OPTIONS = "tracingOptions";
	String TRACING_VIEWER_MAXIMIZED = "maximized";
	
	
	// config file location
	String CONFIG_LOCATION = "configLocation";
}
