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

import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.*;

/**
 * Sets the current target platform based on a target definition.
 * 
 * @since 3.5
 */
public class LoadTargetDefinitionJob extends WorkspaceJob {

	private static final String JOB_FAMILY_ID = "LoadTargetDefinitionJob"; //$NON-NLS-1$

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
	 * the target platform is empty and all other settings are default.  This
	 * method will cancel all existing LoadTargetDefinitionJob instances then
	 * schedules the operation as a user job.
	 * 
	 * @param target target definition or <code>null</code> if none
	 * @return the scheduled job
	 */
	public static LoadTargetDefinitionJob load(ITargetDefinition target) {
		return load(target, null);
	}

	/**
	 * Constructs a new operation to load the specified target definition
	 * as the current target platform. When <code>null</code> is specified
	 * the target platform is empty and all other settings are default.  This
	 * method will cancel all existing LoadTargetDefinitionJob instances then
	 * schedules the operation as a user job.  Adds the given listener to the
	 * job that is started.
	 * 
	 * @param target target definition or <code>null</code> if none
	 * @param listener job change listener that will be added to the created job
	 * @return the scheduled job
	 */
	public static LoadTargetDefinitionJob load(ITargetDefinition target, IJobChangeListener listener) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
		return job;
	}

	/**
	 * Constructs a new operation to load the specified target definition
	 * as the current target platform. When <code>null</code> is specified
	 * the target platform is empty and all other settings are default.
	 *<p>
	 * Clients should use {@link #getLoadJob(ITargetDefinition)} instead to ensure
	 * any existing jobs are cancelled.
	 * </p>
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
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	public boolean belongsTo(Object family) {
		return JOB_FAMILY_ID.equals(family);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
			SubMonitor subMon = SubMonitor.convert(monitor, "", 200); //$NON-NLS-1$

			subMon.subTask(Messages.LoadTargetDefinitionJob_loadEnvironmentSettings);
			loadEnvironment(preferences, subMon.newChild(10));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			loadArgs(preferences, subMon.newChild(10));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			loadJRE(preferences, subMon.newChild(30));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			loadImplicitPlugins(preferences, subMon.newChild(30));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			subMon.subTask(Messages.LoadTargetDefinitionJob_loadTargetPlugins);
			loadPlugins(preferences, subMon.newChild(100));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			PDECore.getDefault().getPreferencesManager().savePluginPreferences();
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			subMon.worked(20);
			subMon.done();
		} catch (CoreException e) {
			return e.getStatus();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Configures program and VM argument preferences based on the target
	 * definition.
	 * 
	 * @param pref preference manager
	 * @param monitor progress monitor
	 */
	private void loadArgs(PDEPreferencesManager pref, IProgressMonitor monitor) {
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
	 * @param pref preference manager
	 * @param monitor progress monitor
	 */
	private void loadEnvironment(PDEPreferencesManager pref, IProgressMonitor monitor) {
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
	 * @param pref preference manager
	 * @param key preference key
	 * @param value preference value or <code>null</code>
	 */
	private void setEnvironmentPref(PDEPreferencesManager pref, String key, String value) {
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
	private void loadJRE(PDEPreferencesManager pref, IProgressMonitor monitor) {
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
	private void loadImplicitPlugins(PDEPreferencesManager pref, IProgressMonitor monitor) {
		NameVersionDescriptor[] infos = fTarget.getImplicitDependencies();
		if (infos != null) {
			monitor.beginTask(Messages.LoadTargetOperation_implicitPluginsTaskName, infos.length + 1);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < infos.length; i++) {
				buffer.append(infos[i].getId()).append(',');
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
	private void loadPlugins(PDEPreferencesManager pref, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMon = SubMonitor.convert(monitor, Messages.LoadTargetOperation_loadPluginsTaskName, 100);
		try {

			// Provision the target to make it local
			IStatus result = fTarget.provision(subMon.newChild(50));
			if (subMon.isCanceled()) {
				return;
			}
			if (result.getSeverity() == IStatus.ERROR) {
				throw new CoreException(result);
			}

			// Create models for the provisioned bundles
			URL[] urls = TargetPlatformHelper.getPluginPaths(fTarget);
			PDEState state = new PDEState(urls, true, subMon.newChild(25));
			if (subMon.isCanceled()) {
				return;
			}

			// Set enablement based on included bundles
			IPluginModelBase[] models = state.getTargetModels();
			for (int i = 0; i < models.length; i++) {
				models[i].setEnabled(true);
			}
			subMon.worked(5);
			if (subMon.isCanceled()) {
				return;
			}

			// For backwards compatibility, continue to set the target home location, additional locations, and realization (profile vs directory) settings
			IBundleContainer[] containers = fTarget.getBundleContainers();
			String home = null;
			Set additional = new HashSet();
			boolean profile = false;
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof AbstractLocalBundleContainer) {
					String location = ((AbstractLocalBundleContainer) containers[i]).getLocation(true);
					if (home == null || home.equals(location)) {
						home = location;
					} else {
						additional.add(location);
					}
				}
				if (!profile && containers[i] instanceof ProfileBundleContainer) {
					profile = true;
				}
			}
			if (home == null) {
				home = TargetPlatform.getDefaultLocation();
			}
			StringBuffer additionalBuf = new StringBuffer();
			for (Iterator iterator = additional.iterator(); iterator.hasNext();) {
				additionalBuf.append((String) iterator.next());
				if (iterator.hasNext()) {
					additionalBuf.append(',');
				}
			}

			pref.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, profile);
			pref.setValue(ICoreConstants.PLATFORM_PATH, home);
			pref.setValue(ICoreConstants.ADDITIONAL_LOCATIONS, additionalBuf.toString());

			// Save the new workspace target handle
			String memento = fTarget.getHandle().getMemento();
			if (fNone) {
				memento = ICoreConstants.NO_TARGET;
			}
			pref.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, memento);

			// Use the TargetPlatformResetJob to update the platform
			Job job = new TargetPlatformResetJob(state);
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
			}
			subMon.worked(20);

		} finally {
			subMon.done();
			monitor.done();
		}
	}
}
