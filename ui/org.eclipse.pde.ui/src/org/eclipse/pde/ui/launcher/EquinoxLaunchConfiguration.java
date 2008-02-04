/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.*;

/**
 * A launch delegate for launching the Equinox framework
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.2
 */
public class EquinoxLaunchConfiguration extends AbstractPDELaunchConfiguration {

	// used to generate the dev classpath entries
	// key is bundle ID, value is a model
	protected Map fAllBundles;

	// key is a model, value is startLevel:autoStart
	private Map fModels;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();

		programArgs.add("-dev"); //$NON-NLS-1$
		programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", fAllBundles)); //$NON-NLS-1$

		saveConfigurationFile(configuration);
		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		String[] args = super.getProgramArguments(configuration);
		for (int i = 0; i < args.length; i++) {
			programArgs.add(args[i]);
		}
		return (String[]) programArgs.toArray(new String[programArgs.size()]);
	}

	private void saveConfigurationFile(ILaunchConfiguration configuration) throws CoreException {
		Properties properties = new Properties();
		properties.setProperty("osgi.install.area", "file:" + TargetPlatform.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("osgi.framework", LaunchConfigurationHelper.getBundleURL("org.eclipse.osgi", fAllBundles)); //$NON-NLS-1$ //$NON-NLS-2$
		int start = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		properties.put("osgi.bundles.defaultStartLevel", Integer.toString(start)); //$NON-NLS-1$
		boolean autostart = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);

		String bundles = null;
		if (fAllBundles.containsKey("org.eclipse.equinox.simpleconfigurator")) { //$NON-NLS-1$
			// If simple configurator is being used, we need to write out the bundles.txt instead of writing out the list in the config.ini
			URL bundlesTxt = P2Utils.writeBundlesTxt(fModels, start, autostart, getConfigDir(configuration));
			if (bundlesTxt != null) {
				properties.setProperty("org.eclipse.equinox.simpleconfigurator.configUrl", bundlesTxt.toString()); //$NON-NLS-1$
			}
			StringBuffer buffer = new StringBuffer();
			IPluginModelBase model = (IPluginModelBase) fAllBundles.get("org.eclipse.equinox.simpleconfigurator"); //$NON-NLS-1$
			buffer.append("reference:"); //$NON-NLS-1$
			buffer.append(LaunchConfigurationHelper.getBundleURL(model));
			appendStartData(buffer, (String) fModels.get(model), autostart);
			bundles = buffer.toString();
		} else {
			bundles = getBundles(autostart);
		}
		if (bundles.length() > 0)
			properties.put("osgi.bundles", bundles); //$NON-NLS-1$

		if (!"3.3".equals(configuration.getAttribute(IPDEUIConstants.LAUNCHER_PDE_VERSION, ""))) { //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("eclipse.ignoreApp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("osgi.noShutdown", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		LaunchConfigurationHelper.save(new File(getConfigDir(configuration), "config.ini"), properties); //$NON-NLS-1$
	}

	private String getBundles(boolean defaultAuto) {
		StringBuffer buffer = new StringBuffer();
		Iterator iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			String id = model.getPluginBase().getId();
			if (!"org.eclipse.osgi".equals(id)) { //$NON-NLS-1$
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append("reference:"); //$NON-NLS-1$
				buffer.append(LaunchConfigurationHelper.getBundleURL(model));

				// fragments must not be started or have a start level
				if (model instanceof IFragmentModel)
					continue;

				String data = fModels.get(model).toString();
				appendStartData(buffer, data, defaultAuto);
			}
		}
		return buffer.toString();
	}

	/**
	 * Convenience method to parses the startData ("startLevel:autoStart"), convert it to the
	 * format expected by the OSGi bundles property, and append to a StringBuffer.
	 * @param buffer buffer to append the data to
	 * @param startData data to parse ("startLevel:autoStart")
	 * @param defaultAuto default auto start setting
	 */
	private void appendStartData(StringBuffer buffer, String startData, boolean defaultAuto) {
		int index = startData.indexOf(':');
		String level = index > 0 ? startData.substring(0, index) : "default"; //$NON-NLS-1$
		String auto = index > 0 && index < startData.length() - 1 ? startData.substring(index + 1) : "default"; //$NON-NLS-1$
		if ("default".equals(auto)) //$NON-NLS-1$
			auto = Boolean.toString(defaultAuto);
		if (!level.equals("default") || "true".equals(auto)) //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("@"); //$NON-NLS-1$

		if (!level.equals("default")) { //$NON-NLS-1$
			buffer.append(level);
			if ("true".equals(auto)) //$NON-NLS-1$
				buffer.append(":"); //$NON-NLS-1$
		}
		if ("true".equals(auto)) { //$NON-NLS-1$
			buffer.append("start"); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		fModels = BundleLauncherHelper.getMergedMap(configuration);
		fAllBundles = new HashMap(fModels.size());
		Iterator iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			fAllBundles.put(model.getPluginBase().getId(), model);
		}

		if (!fAllBundles.containsKey("org.eclipse.osgi")) { //$NON-NLS-1$
			// implicitly add it
			IPluginModelBase model = PluginRegistry.findModel("org.eclipse.osgi"); //$NON-NLS-1$
			if (model != null) {
				fModels.put(model, "default:default"); //$NON-NLS-1$
				fAllBundles.put("org.eclipse.osgi", model); //$NON-NLS-1$
			} else {
				String message = PDEUIMessages.EquinoxLaunchConfiguration_oldTarget;
				throw new CoreException(LauncherUtils.createErrorStatus(message));
			}
		}
		super.preLaunchCheck(configuration, launch, monitor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#validatePluginDependencies(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		OSGiValidationOperation op = new OSGiValidationOperation(configuration);
		LaunchPluginValidator.runValidationOperation(op, monitor);
	}

	/**
	 * Clears the configuration area if the area exists and that option is selected.
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			the progress monitor
	 * @throws CoreException
	 * 			if unable to retrieve launch attribute values
	 * @since 3.3
	 */
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigDir(configuration));
	}

}
