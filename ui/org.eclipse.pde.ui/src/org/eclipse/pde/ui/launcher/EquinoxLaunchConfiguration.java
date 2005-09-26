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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.EquinoxPluginBlock;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;

/**
 * A launch delegate for launching the Equinox framework
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.2
 */
public class EquinoxLaunchConfiguration extends AbstractPDELaunchConfiguration {
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();

		Map workspace = EquinoxPluginBlock.retrieveWorkspaceMap(configuration);
		Map target = EquinoxPluginBlock.retrieveTargetMap(configuration);
		
		Map plugins = getPluginsToRun(workspace, target);
		if (plugins == null)
			return null;
		
		programArgs.add("-dev"); //$NON-NLS-1$
        programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", plugins)); //$NON-NLS-1$

		saveConfigurationFile(configuration, plugins, workspace, target);
		
		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		if (plugins.containsKey("org.eclipse.pde.core")) //$NON-NLS-1$
			programArgs.add("-pdelaunch"); //$NON-NLS-1$

		String[] args = super.getProgramArguments(configuration);
		for (int i = 0; i < args.length; i++) {
			programArgs.add(args[i]);
		}
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	private void saveConfigurationFile(ILaunchConfiguration configuration, Map map, Map workspace, Map target) throws CoreException {
		Properties properties = new Properties();
		properties.setProperty("osgi.install.area", "file:" + ExternalModelManager.getEclipseHome().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("osgi.framework", TargetPlatform.getBundleURL("org.eclipse.osgi", map)); //$NON-NLS-1$ //$NON-NLS-2$
		int start = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		properties.put("osgi.bundles.defaultStartLevel", Integer.toString(start)); //$NON-NLS-1$
		boolean autostart = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		String bundles = getBundles(map, workspace, target, autostart);
		if (bundles.length() > 0)
			properties.put("osgi.bundles", bundles); //$NON-NLS-1$
		properties.put("eclipse.ignoreApp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("osgi.noShutdown", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		LaunchConfigurationHelper.save(new File(getConfigDir(configuration), "config.ini"), properties); //$NON-NLS-1$
	}
	
	private String getBundles(Map plugins, Map workspace, Map target, boolean defaultAuto) {
		StringBuffer buffer = new StringBuffer();
		Iterator iter = plugins.values().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase)iter.next();
			String id = model.getPluginBase().getId();
			if (!"org.eclipse.osgi".equals(id)) { //$NON-NLS-1$
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append("reference:"); //$NON-NLS-1$
				buffer.append(TargetPlatform.getBundleURL(id, plugins));
				
				String data = model.getUnderlyingResource() == null ? target.get(id).toString() : workspace.get(id).toString();
				int index = data.indexOf(':');
				String level = index > 0 ? data.substring(0, index) : "default"; //$NON-NLS-1$
				String auto = index > 0 && index < data.length() - 1 ? data.substring(index + 1) : "default"; //$NON-NLS-1$
				if ("default".equals(auto)) //$NON-NLS-1$
					auto = Boolean.toString(defaultAuto);
				if (!level.equals("default") || "true".equals(auto)) //$NON-NLS-1$ //$NON-NLS-2$
					buffer.append("@"); //$NON-NLS-1$
				
				if (!level.equals("default")) { //$NON-NLS-1$
					buffer.append(level);
					if ("true".equals(auto))  //$NON-NLS-1$
						buffer.append(":"); //$NON-NLS-1$
				}
				if ("true".equals(auto)) { //$NON-NLS-1$
					buffer.append("start"); //$NON-NLS-1$
				}			
			}
		}
		return buffer.toString();
	}
	

	private Map getPluginsToRun(Map workspace, Map target) throws CoreException {
		Map plugins = new TreeMap();
		Iterator iter = workspace.keySet().iterator();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			IPluginModelBase model = manager.findModel(id);
			if (model != null && model.getUnderlyingResource() != null) {
				plugins.put(id, model);
			}
		}
			
		iter = target.keySet().iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			if (!plugins.containsKey(id)) {
				ModelEntry entry = manager.findEntry(id);
				if (entry != null && entry.getExternalModel() != null) {
					plugins.put(id, entry.getExternalModel());
				}
			}
		}
		
		ArrayList statusEntries = new ArrayList();
		iter = plugins.values().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			if (!model.isLoaded()) {
				statusEntries.add(new Status(IStatus.WARNING, 
						PDEPlugin.getPluginId(), 
						IStatus.OK, 
						model.getPluginBase().getId(), 
						null));
			}
		}	
		return LaunchPluginValidator.validatePluginsToRun(plugins, statusEntries);
	}

	/**
	 * checks that the target platform is >= 3.0 before proceeding.
	 * 
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (!TargetPlatform.isOSGi()) {
			IStatus status = new Status(
					IStatus.ERROR, 
					"org.eclipse.pde.ui",  //$NON-NLS-1$
					IStatus.OK,
					PDEUIMessages.EquinoxLaunchConfiguration_oldTarget,
					null);
			throw new CoreException(status);
		}
	}

}
