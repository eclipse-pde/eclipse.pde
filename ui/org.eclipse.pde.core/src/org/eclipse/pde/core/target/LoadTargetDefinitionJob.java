/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.Messages;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

/**
 * Sets the current target platform based on a target definition.
 * 
 * @since 3.8
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
	 */
	public static void load(ITargetDefinition target) {
		load(target, null);
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
	 */
	public static void load(ITargetDefinition target, IJobChangeListener listener) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		Job job = new LoadTargetDefinitionJob(target);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
	}

	/**
	 * Constructs a new operation to load the specified target definition
	 * as the current target platform. When <code>null</code> is specified
	 * the target platform is empty and all other settings are default.
	 *<p>
	 * Clients should use {@link #load(ITargetDefinition, IJobChangeListener)} instead to ensure
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
	@Override
	public boolean belongsTo(Object family) {
		return JOB_FAMILY_ID.equals(family);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
			monitor.beginTask(Messages.LoadTargetOperation_mainTaskName, 20);

			String memento = fTarget.getHandle().getMemento();
			if (fNone) {
				memento = ICoreConstants.NO_TARGET;
			}
			preferences.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, memento);
			((TargetPlatformService) TargetPlatformService.getDefault()).setWorkspaceTargetDefinition(fTarget);
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			clearDeprecatedPreferences(preferences, new SubProgressMonitor(monitor, 3));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			loadJRE(new SubProgressMonitor(monitor, 3));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			PDECore.getDefault().getPreferencesManager().savePluginPreferences();
			resetPlatform(new SubProgressMonitor(monitor, 14));
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Clears any existing target preferences that have been deprecated in 4.4 Luna
	 * 
	 * @param pref preference manager
	 * @param monitor progress monitor
	 */
	@SuppressWarnings("deprecation")
	private void clearDeprecatedPreferences(PDEPreferencesManager pref, IProgressMonitor monitor) {
		String empty = ""; //$NON-NLS-1$
		pref.setValue(ICoreConstants.ARCH, empty);
		pref.setValue(ICoreConstants.NL, empty);
		pref.setValue(ICoreConstants.OS, empty);
		pref.setValue(ICoreConstants.WS, empty);

		pref.setValue(ICoreConstants.PROGRAM_ARGS, empty);
		pref.setValue(ICoreConstants.VM_ARGS, empty);

		pref.setValue(ICoreConstants.TARGET_MODE, empty);
		pref.setValue(ICoreConstants.CHECKED_PLUGINS, empty);
		pref.setValue(ICoreConstants.CHECKED_VERSION_PLUGINS, empty);

		pref.setValue(ICoreConstants.VM_LAUNCHER_INI, empty);
		pref.setValue(ICoreConstants.IMPLICIT_DEPENDENCIES, empty);

		pref.setValue(ICoreConstants.ADDITIONAL_LOCATIONS, empty);
		pref.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, empty);

		pref.setValue(ICoreConstants.POOLED_BUNDLES, empty);
		pref.setValue(ICoreConstants.POOLED_URLS, empty);

		pref.setValue(ICoreConstants.EXTERNAL_FEATURES, empty);
		pref.setValue(ICoreConstants.TARGET_PROFILE, empty);
	}

	/**
	 * Sets the workspace default JRE based on the target's JRE container.
	 *
	 * @param monitor progress monitor
	 */
	private void loadJRE(IProgressMonitor monitor) {
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
	 * Resets the PDE workspace models with the new state information
	 */
	private void resetPlatform(IProgressMonitor monitor) {
		EclipseHomeInitializer.resetEclipseHomeVariable();
		PDECore.getDefault().getSourceLocationManager().reset();
		PDECore.getDefault().getJavadocLocationManager().reset();
		PDECore.getDefault().getExtensionsRegistry().targetReloaded();
		PDECore.getDefault().getFeatureModelManager().targetReloaded();
		PDECore.getDefault().getModelManager().targetReloaded(monitor);
	}

}