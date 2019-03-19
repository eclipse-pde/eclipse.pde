/*******************************************************************************
 *  Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.UpdateManagerHelper;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

/**
 * The central class for the plug-in development target platform. This class cannot
 * be instantiated or subclassed by clients; all functionality is provided
 * by static methods.  Features include:
 * <ul>
 * <li>the target platform's OS/WS/ARCH</li>
 * <li>the default application and product</li>
 * <li>the available applications and products</li>
 * </ul>
 * <p>
 * @since 3.3
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TargetPlatform {

	private static String PRODUCT_PROPERTY = "eclipse.product"; //$NON-NLS-1$
	private static String APPLICATION_PROPERTY = "eclipse.application"; //$NON-NLS-1$

	private static String SDK_PRODUCT = "org.eclipse.sdk.ide"; //$NON-NLS-1$
	private static String PLATFORM_PRODUCT = "org.eclipse.platform.ide"; //$NON-NLS-1$

	private static String IDE_APPLICATION = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	/**
	 * Returns the target platform's main location. As target platforms may contain multiple
	 * locations it is recommended that you use ITargetPlatformService instead.
	 *
	 * @return the target platform's main location
	 */
	public static String getLocation() {
		try {
			ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			ITargetLocation[] containers = target.getTargetLocations();
			// the first container is assumed to be the primary/home location
			String path = null;
			if (containers != null && containers.length > 0) {
				path = containers[0].getLocation(true);
				if (path != null) {
					IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
					path = manager.performStringSubstitution(path);
					return path;
				}
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return getDefaultLocation();
	}

	/**
	 * Returns the location of the default target platform, namely the location
	 * of the host (running) instance of Eclipse.
	 *
	 * @return the location of the default target platform
	 */
	public static String getDefaultLocation() {
		Location location = Platform.getInstallLocation();
		if (location != null) {
			URL url = Platform.getInstallLocation().getURL();
			IPath path = new Path(url.getFile()).removeTrailingSeparator();
			return path.toOSString();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the target operating system as specified on the
	 * <b>Environment</b> tab of the <b>Plug-in Development &gt; Target
	 * Platform</b> preference page.
	 *
	 * @return the target operating system
	 */
	public static String getOS() {
		try {
			ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			String result = target.getOS();
			if (result != null) {
				return result;
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return Platform.getOS();
	}

	/**
	 * Returns the target windowing system as specified on the
	 * <b>Environment</b> tab of the <b>Plug-in Development &gt; Target
	 * Platform</b> preference page.
	 *
	 * @return the target windowing system
	 */
	public static String getWS() {
		try {
			ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			String result = target.getWS();
			if (result != null) {
				return result;
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return Platform.getWS();
	}

	/**
	 * Returns the target locale as specified on the <b>Environment</b> tab of
	 * the <b>Plug-in Development &gt; Target Platform</b> preference page.
	 *
	 * @return the target locale
	 */
	public static String getNL() {
		try {
			ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			String result = target.getNL();
			if (result != null) {
				return result;
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return Platform.getNL();
	}

	/**
	 * Returns the target system architecture as specified on the
	 * <b>Environment</b> tab of the <b>Plug-in Development &gt; Target
	 * Platform</b> preference page.
	 *
	 * @return the target system architecture
	 */
	public static String getOSArch() {
		try {
			ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			String result = target.getArch();
			if (result != null) {
				return result;
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return Platform.getOSArch();
	}

	/**
	 * Returns a list of identifiers for all available applications
	 * (i.e. <code>org.eclipse.core.runtime.applications</code> extensions) declared in the workspace
	 * and target platform plug-ins.
	 * <p>
	 * If a workspace plug-in has the same ID as a plug-in in the target platform, the extensions
	 * declared in the target counterpart are ignored.
	 * </p>
	 *
	 * @return a list of identifiers for all available applications
	 */
	public static String[] getApplications() {
		return TargetPlatformHelper.getApplicationNames();
	}

	/**
	 * Returns a list of identifiers for all available products
	 * (i.e. <code>org.eclipse.core.runtime.products</code> extensions) declared in the workspace
	 * and target platform plug-ins.
	 * <p>
	 * If a workspace plug-in has the same ID as a plug-in in the target platform, the extensions
	 * declared in the target counterpart are ignored.
	 * </p>
	 *
	 * @return a list of identifiers for all available products
	 */
	public static String[] getProducts() {
		return TargetPlatformHelper.getProductNames();
	}

	/**
	 * Returns the ID for the default product
	 * (<code>org.eclipse.core.runtime.products</code> extension) for the current target platform,
	 * or <code>null</code> if none can be determined.
	 *
	 * If any of the
	 *
	 * @return the ID for the default product or <code>null</code> if none could be determined
	 */
	public static String getDefaultProduct() {
		Properties config = TargetPlatformHelper.getConfigIniProperties();
		Set<String> set = TargetPlatformHelper.getProductNameSet();
		if (config != null) {
			String product = (String) config.get(PRODUCT_PROPERTY);
			if (product != null && set.contains(product)) {
				return product;
			}
		}

		if (set.contains(SDK_PRODUCT)) {
			return SDK_PRODUCT;
		}

		return set.contains(PLATFORM_PRODUCT) ? PLATFORM_PRODUCT : null;
	}

	/**
	 * Returns the ID for the default application
	 * (<code>org.eclipse.core.runtime.applications</code> extension) for the current target
	 * platform.
	 * <p>
	 * If none could be determined, then <code>org.eclipse.ui.ide.workbench</code>
	 * application is returned.
	 * </p>
	 * @return the default application to run when launching an Eclipse application
	 */
	public static String getDefaultApplication() {
		Properties config = TargetPlatformHelper.getConfigIniProperties();
		Set<String> set = TargetPlatformHelper.getApplicationNameSet();
		if (config != null) {
			String application = (String) config.get(APPLICATION_PROPERTY);
			if (application != null && set.contains(application)) {
				return application;
			}
		}
		return IDE_APPLICATION;
	}

	/**
	 * Creates a platform configuration to be used when launching an Eclipse
	 * application that uses Update Manager as a configurator
	 *
	 * @param location
	 *            the location where the configuration should be persisted
	 * @param plugins
	 *            the list of plug-ins that make up the configuration
	 * @param brandingPlugin
	 *            if specified, a entry for the feature containing the branding
	 *            plug-in will be created in the platform configuration
	 *
	 * @throws CoreException
	 *             an exception is thrown if there was a problem writing the
	 *             platform configuration file
	 * @deprecated Old Update Manager is planned for removal and so it this API.
	 * @noreference Old Update Manager is planned for removal and so it this API.
	 *              See bug 527378 for details.
	 */
	@Deprecated
	public static void createPlatformConfiguration(File location, IPluginModelBase[] plugins, IPluginModelBase brandingPlugin) throws CoreException {
		UpdateManagerHelper.createPlatformConfiguration(location, plugins, brandingPlugin);
	}

	/**
	 * The comma-separated list of bundles which are automatically installed and
	 * optionally started.
	 * <p>
	 * Each entry if of the form &lt;bundleID&gt;[@ [&lt;startlevel&gt;]
	 * [":start"]] If the startlevel is omitted then the framework will use the
	 * default start level for the bundle. If the "start" tag is added then the
	 * bundle will be marked as started after being installed.
	 * </p>
	 * <p>
	 * The list computed is based on the <b>osgi.bundles</b> key found in the
	 * config.ini file of the target platform. If no such key is found, then a
	 * suitable list is computed based on the target platform version.
	 * </p>
	 *
	 * @return a comma-separated list of bundles that are automatically
	 *         installed and optionally started when a runtime Eclipse
	 *         application is launched.
	 */
	public static String getBundleList() {
		return TargetPlatformHelper.getBundleList();
	}

}
