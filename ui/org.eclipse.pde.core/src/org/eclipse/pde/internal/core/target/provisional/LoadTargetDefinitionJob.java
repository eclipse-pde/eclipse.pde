/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.provisional;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.impl.Messages;
import org.eclipse.pde.internal.core.target.impl.ProfileBundleContainer;

/**
 * Sets the current target platform based on a target definition.
 * 
 * @since 3.5
 */
public class LoadTargetDefinitionJob extends WorkspaceJob {

	/**
	 * Target definition being loaded
	 */
	private ITargetDefinition fTarget;

	/**
	 * Whether a target definition was specified
	 */
	private boolean fNone = false;

	/**
	 * Constructs a new operation to load the specified target definition
	 * as the current target platform. When <code>null</code> is specified
	 * the target platform is empty and all other settings are default.
	 * 
	 * @param target target definition or <code>null</code> if none
	 */
	public LoadTargetDefinitionJob(ITargetDefinition target) {
		super(Messages.LoadTargetDefinitionJob_0);
		fTarget = target;
		if (target == null) {
			fNone = true;
			ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			fTarget = service.newTarget();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			Preferences preferences = PDECore.getDefault().getPluginPreferences();
			monitor.beginTask(Messages.LoadTargetOperation_mainTaskName, 100);
			loadEnvironment(preferences, new SubProgressMonitor(monitor, 5));
			loadArgs(preferences, new SubProgressMonitor(monitor, 5));
			loadJRE(preferences, new SubProgressMonitor(monitor, 15));
			loadImplicitPlugins(preferences, new SubProgressMonitor(monitor, 15));
			loadPlugins(preferences, new SubProgressMonitor(monitor, 60));
			loadAdditionalPreferences(preferences);
			PDECore.getDefault().savePluginPreferences();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Configures program and VM argument preferences based on the target
	 * definition.
	 * 
	 * @param pref preference store
	 * @param monitor progress monitor
	 */
	private void loadArgs(Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(Messages.LoadTargetOperation_argsTaskName, 2);
		String args = fTarget.getProgramArguments();
		pref.setValue(ICoreConstants.PROGRAM_ARGS, (args != null) ? args : ""); //$NON-NLS-1$
		monitor.worked(1);
		args = fTarget.getVMArguments();
		pref.setValue(ICoreConstants.VM_ARGS, (args != null) ? args : ""); //$NON-NLS-1$
		monitor.done();
	}

	/**
	 * Configures the environment preferences from the target definition.
	 * 
	 * @param pref preference store
	 * @param monitor progress monitor
	 */
	private void loadEnvironment(Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(Messages.LoadTargetOperation_envTaskName, 1);
		setEnvironmentPref(pref, ICoreConstants.ARCH, fTarget.getArch());
		setEnvironmentPref(pref, ICoreConstants.NL, fTarget.getNL());
		setEnvironmentPref(pref, ICoreConstants.OS, fTarget.getOS());
		setEnvironmentPref(pref, ICoreConstants.WS, fTarget.getWS());
		monitor.done();
	}

	/**
	 * Sets the given preference to default when <code>null</code> or the
	 * specified value.
	 * 
	 * @param pref preference store
	 * @param key preference key
	 * @param value preference value or <code>null</code>
	 */
	private void setEnvironmentPref(Preferences pref, String key, String value) {
		if (value == null) {
			pref.setToDefault(key);
		} else {
			pref.setValue(key, value);
		}
	}

	/**
	 * Sets the workspace default JRE based on the target's JRE container.
	 *
	 * @param pref
	 * @param monitor
	 */
	private void loadJRE(Preferences pref, IProgressMonitor monitor) {
		IPath container = fTarget.getJREContainer();
		monitor.beginTask(Messages.LoadTargetOperation_jreTaskName, 1);
		if (container != null) {
			IVMInstall jre = JavaRuntime.getVMInstall(container);
			if (jre != null) {
				IVMInstall def = JavaRuntime.getDefaultVMInstall();
				if (!jre.equals(def)) {
					try {
						JavaRuntime.setDefaultVMInstall(jre, null);
					} catch (CoreException e) {
					}
				}
			}
		}
		monitor.done();
	}

	/**
	 * Sets implicit dependencies, if any
	 * 
	 * @param pref preference store
	 * @param monitor progress monitor
	 */
	private void loadImplicitPlugins(Preferences pref, IProgressMonitor monitor) {
		BundleInfo[] infos = fTarget.getImplicitDependencies();
		if (infos != null) {
			monitor.beginTask(Messages.LoadTargetOperation_implicitPluginsTaskName, infos.length + 1);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < infos.length; i++) {
				buffer.append(infos[i].getSymbolicName()).append(',');
				monitor.worked(1);
			}
			if (infos.length > 0)
				buffer.setLength(buffer.length() - 1);
			pref.setValue(ICoreConstants.IMPLICIT_DEPENDENCIES, buffer.toString());
		}
		monitor.done();
	}

	/**
	 * Resolves the bundles in the target platform and sets them in the corresponding
	 * CHECKED_PLUGINS preference. Sets home and addition location preferences as well.
	 * 
	 * @param pref
	 * @param monitor
	 * @throws CoreException
	 */
	private void loadPlugins(Preferences pref, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.LoadTargetOperation_loadPluginsTaskName, 100);
		String currentPath = pref.getString(ICoreConstants.PLATFORM_PATH);
		IBundleContainer[] containers = fTarget.getBundleContainers();
		// the first container is assumed to be the primary/home location
		String path = null;
		if (containers != null && containers.length > 0) {
			path = containers[0].getHomeLocation();
		}
		if (path == null) {
			path = TargetPlatform.getDefaultLocation();
		} else {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				path = manager.performStringSubstitution(path);
			} catch (CoreException e) {
				return;
			}
		}
		monitor.worked(10);
		List additional = getAdditionalLocs();
		handleReload(path, additional, pref, new SubProgressMonitor(monitor, 85));

		// update preferences (Note: some preferences updated in handleReload())
		pref.setValue(ICoreConstants.PLATFORM_PATH, path);
		String mode = new Path(path).equals(new Path(TargetPlatform.getDefaultLocation())) ? ICoreConstants.VALUE_USE_THIS : ICoreConstants.VALUE_USE_OTHER;
		pref.setValue(ICoreConstants.TARGET_MODE, mode);

		ListIterator li = additional.listIterator();
		StringBuffer buffer = new StringBuffer();
		while (li.hasNext())
			buffer.append(li.next()).append(","); //$NON-NLS-1$
		if (buffer.length() > 0)
			buffer.setLength(buffer.length() - 1);
		pref.setValue(ICoreConstants.ADDITIONAL_LOCATIONS, buffer.toString());

		String newValue = currentPath;
		for (int i = 0; i < 4; i++) {
			String value = pref.getString(ICoreConstants.SAVED_PLATFORM + i);
			pref.setValue(ICoreConstants.SAVED_PLATFORM + i, newValue);
			if (!value.equals(currentPath))
				newValue = value;
			else
				break;
		}
		monitor.done();
	}

	/**
	 * Sets the TARGET_PROFILE preference which stores the ID of the target profile used 
	 * (if based on an target extension) or the workspace location of the file that
	 * was used. For now we just clear it.
	 * <p>
	 * Sets the WORKSPACE_TARGET_HANDLE.
	 * </p>
	 * @param pref
	 */
	private void loadAdditionalPreferences(Preferences pref) throws CoreException {
		pref.setValue(ICoreConstants.TARGET_PROFILE, ""); //$NON-NLS-1$
		String memento = fTarget.getHandle().getMemento();
		if (fNone) {
			memento = ""; //$NON-NLS-1$
		}
		pref.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, memento);
		IBundleContainer[] containers = fTarget.getBundleContainers();
		boolean profile = false;
		if (containers.length > 0) {
			profile = containers[0] instanceof ProfileBundleContainer;
		}
		pref.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, profile);
	}

	/**
	 * Returns a list of additional locations of bundles.
	 * 
	 * @return additional bundle locations
	 */
	private List getAdditionalLocs() throws CoreException {
		ArrayList additional = new ArrayList();
		// secondary containers are considered additional
		IBundleContainer[] containers = fTarget.getBundleContainers();
		if (containers != null && containers.length > 1) {
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			for (int i = 1; i < containers.length; i++) {
				try {
					additional.add(manager.performStringSubstitution(containers[i].getHomeLocation()));
				} catch (CoreException e) {
					additional.add(containers[i].getHomeLocation());
				}
			}
		}
		return additional;
	}

	private void handleReload(String targetLocation, List additionalLocations, Preferences pref, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.LoadTargetOperation_reloadTaskName, 85);

		List infos = new ArrayList();
		BundleInfo[] code = fTarget.resolveBundles(null);
		for (int i = 0; i < code.length; i++) {
			infos.add(code[i]);
		}
		// to be consistent with previous implementation, add source bundles
		BundleInfo[] sourceBundles = fTarget.resolveSourceBundles(null);
		for (int i = 0; i < sourceBundles.length; i++) {
			infos.add(sourceBundles[i]);
		}
		BundleInfo[] bundles = (BundleInfo[]) infos.toArray(new BundleInfo[infos.size()]);
		// generate URLs and save CHECKED_PLUGINS
		StringBuffer checked = new StringBuffer();

		URL[] paths = new URL[bundles.length];
		for (int i = 0; i < paths.length; i++) {
			try {
				paths[i] = new File(bundles[i].getLocation()).toURL();
				if (i > 0) {
					checked.append(" "); //$NON-NLS-1$
				}
				checked.append(bundles[i].getSymbolicName());
			} catch (MalformedURLException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LoadTargetDefinitionJob_1, e));
			}
		}

		PDEState state = new PDEState(paths, true, new SubProgressMonitor(monitor, 45));
		IPluginModelBase[] models = state.getTargetModels();
		for (int i = 0; i < models.length; i++) {
			models[i].setEnabled(true);
		}
		// save CHECKED_PLUGINS
		if (paths.length == 0) {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_NONE);
		} else {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, checked.toString());
		}

		Job job = new TargetPlatformResetJob(state);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
		monitor.done();
	}

}
