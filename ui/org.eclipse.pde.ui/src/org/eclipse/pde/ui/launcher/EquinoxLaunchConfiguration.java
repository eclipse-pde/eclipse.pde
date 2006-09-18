/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.launcher.OSGiBundleBlock;
import org.eclipse.swt.widgets.Display;

/**
 * A launch delegate for launching the Equinox framework
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.2
 */
public class EquinoxLaunchConfiguration extends AbstractPDELaunchConfiguration {
	
	private Map fWorskspaceBundles;
	private Map fTargetBundles;
	
	protected Map fAllBundles;

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

		if (fAllBundles.containsKey("org.eclipse.pde.core")) //$NON-NLS-1$
			programArgs.add("-pdelaunch"); //$NON-NLS-1$

		String[] args = super.getProgramArguments(configuration);
		for (int i = 0; i < args.length; i++) {
			programArgs.add(args[i]);
		}
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	private void saveConfigurationFile(ILaunchConfiguration configuration) throws CoreException {
		Properties properties = new Properties();
		properties.setProperty("osgi.install.area", "file:" + ExternalModelManager.getEclipseHome().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("osgi.framework", LaunchConfigurationHelper.getBundleURL("org.eclipse.osgi", fAllBundles)); //$NON-NLS-1$ //$NON-NLS-2$
		int start = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		properties.put("osgi.bundles.defaultStartLevel", Integer.toString(start)); //$NON-NLS-1$
		boolean autostart = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		String bundles = getBundles(autostart);
		if (bundles.length() > 0)
			properties.put("osgi.bundles", bundles); //$NON-NLS-1$
		properties.put("eclipse.ignoreApp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("osgi.noShutdown", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		LaunchConfigurationHelper.save(new File(getConfigDir(configuration), "config.ini"), properties); //$NON-NLS-1$
	}
	
	private String getBundles(boolean defaultAuto) {
		StringBuffer buffer = new StringBuffer();
		Iterator iter = fAllBundles.values().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase)iter.next();
			String id = model.getPluginBase().getId();
			if (!"org.eclipse.osgi".equals(id)) { //$NON-NLS-1$
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append("reference:"); //$NON-NLS-1$
				buffer.append(LaunchConfigurationHelper.getBundleURL(id, fAllBundles));
				
				String data = model.getUnderlyingResource() == null 
							? fTargetBundles.get(id).toString() 
							: fWorskspaceBundles.get(id).toString();
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
		
	private Map getBundlesToRun(Map workspace, Map target) {
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
		return plugins;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		fWorskspaceBundles = OSGiBundleBlock.retrieveWorkspaceMap(configuration);
		fTargetBundles = OSGiBundleBlock.retrieveTargetMap(configuration);	
		fAllBundles = getBundlesToRun(fWorskspaceBundles, fTargetBundles);
		super.preLaunchCheck(configuration, launch, monitor);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#validatePluginDependencies(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		if (!fAllBundles.containsKey("org.eclipse.osgi")) { //$NON-NLS-1$
			final Display display = LauncherUtils.getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(display.getActiveShell(), 
							PDEUIMessages.LauncherUtils_title, 
							PDEUIMessages.EquinoxLaunchConfiguration_oldTarget);
				}
			});
		}
		IPluginModelBase[] models = (IPluginModelBase[])fAllBundles.values().toArray(new IPluginModelBase[fAllBundles.size()]);
		LaunchPluginValidator.validatePluginDependencies(models, monitor);
	}	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#clear(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigDir(configuration));	
	}

}
