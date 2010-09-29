/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.launching;

/**
 * Constant definitions for PDE launch configurations.
 * <p>
 * Constant definitions only; not to be implemented.
 * </p>
 * <p>
 * This class originally existed in 3.2 as
 * <code>org.eclipse.pde.ui.launcher.IPDELauncherConstants</code>.
 * </p>
 * @since 3.6
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
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
	 * 
	 * @deprecated use IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH
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
	 * 
	 * @deprecated As of 3.6 the feature-based self-hosting option is not supported
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
	 * 
	 * @deprecated This option is no longer supported in the launch config.  A recent selection is stored
	 * in dialog settings.
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
	String CONFIG_TEMPLATE_LOCATION = "templateConfig"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a string specifying 
	 * the location of the .product file with which this launch configuration 
	 * is associated.
	 */
	String PRODUCT_FILE = "productFile"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is the ID of an OSGi framework
	 * declared in an <code>org.eclipse.pde.ui.osgiLaunchers</code> extension point.
	 * 
	 * @since 3.3
	 */
	String OSGI_FRAMEWORK_ID = "osgi_framework_id"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a boolean specifying
	 * if the default Auto-Start for an OSGi Framework launch configuration 
	 * is <code>true</code> or <code>false</code>
	 * 
	 * @see IPDELauncherConstants#DEFAULT_START_LEVEL
	 */
	String DEFAULT_AUTO_START = "default_auto_start"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is an integer specifying
	 * the default start level for bundles in an OSGi Framework launch configuration.
	 * 
	 * @see IPDELauncherConstants#DEFAULT_AUTO_START
	 */
	String DEFAULT_START_LEVEL = "default_start_level"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a comma-separated list
	 * of workspace bundles to launch with the OSGi framework.
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
	 * of non-workspace bundles to launch with the OSGi framework.
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

	/**
	 * Launch configuration attribute key. The value is a boolean indicating
	 * whether or not to display only selected plug-ins. 
	 * 
	 * @since 3.4
	 */
	String SHOW_SELECTED_ONLY = "show_selected_only"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the bundles tab
	 * 
	 * @since 3.5
	 */
	String TAB_BUNDLES_ID = "org.eclipse.pde.ui.launch.tab.osgi.bundles"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the configuration tab
	 * 
	 * @since 3.5
	 */
	String TAB_CONFIGURATION_ID = "org.eclipse.pde.ui.launch.tab.configuration"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the main tab
	 * 
	 * @since 3.5
	 */
	String TAB_MAIN_ID = "org.eclipse.pde.ui.launch.tab.main"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the osgi settings tab
	 * 
	 * @since 3.5
	 */
	String TAB_OSGI_SETTINGS_ID = "org.eclipse.pde.ui.launch.tab.osgi.settings"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the plug-in junit tab
	 * 
	 * @since 3.5
	 */
	String TAB_PLUGIN_JUNIT_MAIN_ID = "org.eclipse.pde.ui.launch.tab.junit.main"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the plug-ins tab
	 * 
	 * @since 3.5
	 */
	String TAB_PLUGINS_ID = "org.eclipse.pde.ui.launch.tab.plugins"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the tracing tab
	 * 
	 * @since 3.5
	 */
	String TAB_TRACING_ID = "org.eclipse.pde.ui.launch.tab.tracing"; //$NON-NLS-1$

	/**
	 * The unique tab identifier for the tracing tab
	 * 
	 * @since 3.5
	 */
	String TAB_TEST_ID = "org.eclipse.pde.ui.launch.tab.test"; //$NON-NLS-1$

	/**
	 * The launch configuration type id for OSGi launches.
	 * 
	 * @since 3.5
	 */
	String OSGI_CONFIGURATION_TYPE = "org.eclipse.pde.ui.EquinoxLauncher"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a boolean specifying 
	 * whether the tests should run on the UI thread.
	 * 
	 * The default value is <code>true</code>
	 * 
	 * @since 3.5
	 */
	String RUN_IN_UI_THREAD = "run_in_ui_thread"; //$NON-NLS-1$

	/**
	 * The launch configuration type for Eclipse application launches.
	 * 
	 * @since 3.6
	 */
	String ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.pde.ui.RuntimeWorkbench"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key.  The value is a boolean specifying
	 * whether a p2 profile should be 
	 * 
	 * @since 3.6
	 */
	String GENERATE_PROFILE = "generateProfile"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a List specifying the features
	 * to include when launching (when {@link #USE_CUSTOM_FEATURES} is set to <code>true</code>.
	 * The values in the List are strings that contain the id and plugin resolution value as follows:
	 * <pre>
	 * [feature_id]:[resolution]
	 * </pre>
	 * The resolution must be one of {@link #LOCATION_DEFAULT}, {@link #LOCATION_EXTERNAL}, {@link #LOCATION_WORKSPACE}
	 *  
	 * @since 3.6
	 */
	String SELECTED_FEATURES = "selected_features"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * if the feature-based launching mode should be used.
	 * This mode will launch with all the workspace and external features
	 * that have been explicitly selected in the Plug-ins Tab.
	 * 
	 *  @since 3.6
	 */
	String USE_CUSTOM_FEATURES = "useCustomFeatures"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a String specifying
	 * if the default location for a feature is {@link #LOCATION_WORKSPACE} 
	 * or {@link #LOCATION_EXTERNAL} 
	 * 
	 *  @since 3.6
	 */
	String FEATURE_DEFAULT_LOCATION = "featureDefaultLocation"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a String specifying
	 * if the default plug-in resolution location for a feature 
	 * is {@link #LOCATION_WORKSPACE} or {@link #LOCATION_EXTERNAL} 
	 * 
	 *  @since 3.6
	 */
	String FEATURE_PLUGIN_RESOLUTION = "featurePluginResolution"; //$NON-NLS-1$

	/**
	 * Value for a launch configuration attribute used when the object should be
	 * obtained from whatever the default location is for this works
	 * 
	 * @since 3.6
	 * @see #FEATURE_PLUGIN_RESOLUTION
	 */
	String LOCATION_DEFAULT = "default"; //$NON-NLS-1$

	/**
	 * Value for a launch configuration attribute used when the object should
	 * be obtained from an external location over the workspace.
	 * 
	 * @since 3.6
	 * @see #FEATURE_DEFAULT_LOCATION
	 * @see #FEATURE_PLUGIN_RESOLUTION
	 */
	String LOCATION_EXTERNAL = "external"; //$NON-NLS-1$

	/**
	 * Value for a launch configuration attribute used when the object should
	 * be obtained from the workspace over an external location.
	 * 
	 * @since 3.6
	 * @see #FEATURE_DEFAULT_LOCATION
	 * @see #FEATURE_PLUGIN_RESOLUTION
	 */
	String LOCATION_WORKSPACE = "workspace"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a List specifying the additional plug-ins that 
	 * will be included along with the features in the list {@link #SELECTED_FEATURES}
	 * when launching (when {@link #USE_CUSTOM_FEATURES} is set to <code>true</code>.
	 * The values in the List are strings that contain the id and versions as follows:
	 * <pre>
	 * [plugin_id]:[version]
	 * </pre>
	 *  
	 * @since 3.6
	 */
	String ADDITIONAL_PLUGINS = "additional_plugins"; //$NON-NLS-1$
}
