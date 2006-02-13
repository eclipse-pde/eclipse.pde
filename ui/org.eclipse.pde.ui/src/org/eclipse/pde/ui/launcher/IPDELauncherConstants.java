/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

/**
 * Constant definitions for PDE launch configurations.
 * <p>
 * Constant definitions only; not to be implemented.
 * </p>
 * @since 3.2
 */
public interface IPDELauncherConstants {
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * workspace data location for an Eclipse application.
	 */
	String LOCATION = "location"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * workspace data location for an Eclipse application should be cleared
	 * prior to launching.
	 */
	String DOCLEAR = "clearws"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * whether the user should be prompted prior to clearing the workspace.
	 * 
	 * @see IPDELauncherConstants#DOCLEAR
	 */	
	String ASKCLEAR = "askclear"; //$NON-NLS-1$
		
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * the application to run.  If the value is <code>null</code>, the default 
	 * application as specified in the target platform will be used.
	 */	
	String APPLICATION = "application"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * the product to run.
	 * 
	 * @see IPDELauncherConstants#APPLICATION
	 */		
	String PRODUCT = "product"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * if the launch should appear in product-mode.  If the value is <code>false</code>,
	 * the launch takes place in application-mode.
	 * 
	 * @see IPDELauncherConstants#PRODUCT
	 * @see IPDELauncherConstants#APPLICATION
	 */	
	String USE_PRODUCT = "useProduct"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key used in Plug-in JUnit launch configurations only. 
	 * The value is a string specifying the application to be tested.  
	 * If the value is <code>null</code>, the default UI workbench application is tested.
	 */		
	String APP_TO_TEST = "testApplication"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * the name of the VM to launch with.  If the value is <code>null</code>,
	 * the default workspace VM is used.
	 */	
	String VMINSTALL = "vminstall"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * the user-entered bootstrap classpath entries. 
	 */	
	String BOOTSTRAP_ENTRIES = "bootstrap"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * if the default self-hosting mode should be used when launching.
	 * The default being to launch with all workspace plug-ins and all the 
	 * plug-ins that are explicitly checked on the Target Platform preference page.
	 */	
	String USE_DEFAULT = "default"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * if the feature-based self-hosting mode should be used.
	 * The workspace must be set up properly for the feature-based self-hosting
	 * to succeed.
	 * Check the PDE Tips and Tricks section for how to set up feature-based self-hosting.
	 */		
	String USEFEATURES = "usefeatures"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * a comma-separated list of IDs of workspace plug-ins to launch with.
	 * This value is only used when the Automatic Add option is off.
	 * 
	 * @see IPDELauncherConstants#AUTOMATIC_ADD
	 */				
	String SELECTED_WORKSPACE_PLUGINS = "selected_workspace_plugins"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * a comma-separated list of IDs of workspace plug-ins that are to be excluded from
	 * the launch.
	 * This value is only used when the Automatic Add option is on.
	 * 
	 * @see IPDELauncherConstants#AUTOMATIC_ADD
	 */					
	String DESELECTED_WORKSPACE_PLUGINS = "deselected_workspace_plugins"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * whether workspace plug-in created after the creation of a launch configuration
	 * should be added to the list of plug-ins to launch with.
	 * 
	 * If the value is <code>true</code>, then DESELECTED_WORKSPACE_PLUGINS should be used.
	 * Otherwise, SELECTED_WORKSPACE_PLUGINS should be used.
	 * 
	 * @see IPDELauncherConstants#DESELECTED_WORKSPACE_PLUGINS
	 * @see IPDELauncherConstants#SELECTED_WORKSPACE_PLUGINS
	 */					
	String AUTOMATIC_ADD = "automaticAdd"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * whether the list of plug-ins to run should be validate prior to launching.
	 * If problems are found, they will be reported and the user will be able to cancel or
	 * continue.
	 * If no problems are found, the launch continues as normal.
	 */					
	String AUTOMATIC_VALIDATE = "automaticValidate"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying
	 * a comma-separated list of IDs of target platform plug-ins to launch with.
	 * This value is only used when the Automatic Add option is off.
	 */					
	String SELECTED_TARGET_PLUGINS = "selected_target_plugins"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean indicating
	 * whether the computation of required plug-ins on the Plug-ins tab should include
	 * the traversal of optional dependencies.
	 */		
	String INCLUDE_OPTIONAL = "includeOptional"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean indicating
	 * whether tracing is enabled or disabled.
	 */		
	String TRACING = "tracing"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a map containing the list
	 * of options to debug with.
	 */			
	String TRACING_OPTIONS = "tracingOptions"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is the id of the last plug-in 
	 * that was selected on the Tracing tab.
	 */				
	String TRACING_SELECTED_PLUGIN = "selectedPlugin"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is the IDs of all plug-ins
	 * checked on the Tracing tab.  The value may also be "[NONE]"
	 * 
	 * @see IPDELauncherConstants#TRACING_NONE
	 */
	String TRACING_CHECKED = "checked"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute value indicating that, although tracing is enabled,
	 * no plug-ins have been selected to be traced.
	 */
	String TRACING_NONE = "[NONE]"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying 
	 * if PDE should generate a default configuration area for the launch.
	 * 
	 * If <code>true</code>, a configuration location in the PDE metadata area 
	 * is created.  Otherwise, the user is expected to specify a location.
	 * 
	 * @see IPDELauncherConstants#CONFIG_LOCATION
	 */				
	String CONFIG_USE_DEFAULT_AREA = "useDefaultConfigArea"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a string specifying 
	 * the configuration area location for an Eclipse application launch.
	 * 
	 * This key is only used when CONFIG_USE_DEFAULT_AREA is <code>false</code>.
	 * 
	 * @see IPDELauncherConstants#CONFIG_USE_DEFAULT_AREA
	 */				
	String CONFIG_LOCATION = "configLocation"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a boolean specifying 
	 * if the configuration area location should be cleared prior to launching
	*/
	String CONFIG_CLEAR_AREA = "clearConfig"; //$NON-NLS-1$
	
	/**
	 * Launch configuration atribute key.  The value is a boolean specifying
	 * if PDE should generate a default config.ini file for the launch.
	 * 
	 * If <code>true</code>, a configuration file is created.  
	 * Otherwise, the user is expected to specify a config.ini to be used as a template.
	 * 
	 * @see IPDELauncherConstants#CONFIG_TEMPLATE_LOCATION
	 */
	String CONFIG_GENERATE_DEFAULT = "useDefaultConfig"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying 
	 * the location of the config.ini file to be used as a template for an 
	 * Eclipse application launch.
	 * 
	 * This key is only used when CONFIG_GENERATE_DEFAULT is <code>false</code>.
	 * 
	 * @see IPDELauncherConstants#CONFIG_GENERATE_DEFAULT
	 */					
	String CONFIG_TEMPLATE_LOCATION = "templateConfig";	 //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key. The value is a string specifying 
	 * the location of the .product file with which this launch configuration 
	 * is associated.
	 */					
	String PRODUCT_FILE = "productFile"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a boolean specifying
	 * if the default Auto-Start for an Equinox Framework launch configuration 
	 * is <code>true</code> or <code>false</code>
	 * 
	 * @see IPDELauncherConstants#DEFAULT_START_LEVEL
	 */
	String DEFAULT_AUTO_START = "default_auto_start"; //$NON-NLS-1$
	
	/**
	 * Launch configuration attribute key.  The value is an integer specifying
	 * the default start level for bundles in an Equinox Framework launch configuration.
	 * 
	 * @see IPDELauncherConstants#DEFAULT_AUTO_START
	 */
	String DEFAULT_START_LEVEL = "default_start_level"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a comma-separated list
	 * of workspace bundles to launch with the Equinox framework.
	 * 
	 * Each token in the list is of the format:
	 * <plugin-id>@<start-level>:<auto-start>
	 * 
	 * @see IPDELauncherConstants#DEFAULT_AUTO_START
	 * @see IPDELauncherConstants#DEFAULT_START_LEVEL
	 */
	String WORKSPACE_BUNDLES = "workspace_bundles"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a comma-separated list
	 * of non-workspace bundles to launch with the Equinox framework.
	 * 
	 * Each token in the list is of the format:
	 * <plugin-id>@<start-level>:<auto-start>
	 * 
	 * @see IPDELauncherConstants#DEFAULT_AUTO_START
	 * @see IPDELauncherConstants#DEFAULT_START_LEVEL
	 */
	String TARGET_BUNDLES = "target_bundles"; //$NON-NLS-1$
			
	/**
	 * Launch configuration attribute key.  The value can be either the full path
	 * to the workspace location of a Target Definition (ie. .target file), or
	 * the ID of a target defined in an org.eclipse.pde.core.targets extension.
	 */
	String DEFINED_TARGET = "defined_target"; //$NON-NLS-1$
}
