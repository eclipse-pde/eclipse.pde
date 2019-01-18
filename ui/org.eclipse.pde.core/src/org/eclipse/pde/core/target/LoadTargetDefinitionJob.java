/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 540881
 *******************************************************************************/
package org.eclipse.pde.core.target;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.EclipseHomeInitializer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
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
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			fTarget = service.newTarget();
		}
	}


	@Override
	public boolean belongsTo(Object family) {
		return JOB_FAMILY_ID.equals(family);
	}


	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			SubMonitor subMon = SubMonitor.convert(monitor, Messages.LoadTargetOperation_mainTaskName, 40)
					.checkCanceled();

			if (!fTarget.isResolved()) {
				fTarget.resolve(subMon.split(20));
			}
			subMon.checkCanceled();
			subMon.setWorkRemaining(20);

			PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();

			((TargetPlatformService) TargetPlatformService.getDefault()).setWorkspaceTargetDefinition(fTarget); // Must be set before preference so listeners can react
			String memento = fTarget.getHandle().getMemento();
			if (fNone) {
				memento = ICoreConstants.NO_TARGET;
			}
			// If the same target has been modified, clear the preference so listeners can react to the change
			if (memento.equals(preferences.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE))) {
				preferences.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, ""); //$NON-NLS-1$
			}
			preferences.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, memento);

			clearDeprecatedPreferences(preferences, subMon.split(3));

			loadJRE(subMon.split(3));

			PDECore.getDefault().getPreferencesManager().savePluginPreferences();
			resetPlatform(subMon.split(14));

		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
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
		PDECore.getDefault().getModelManager().targetReloaded(monitor); // PluginModelManager should be reloaded first to reset isCancelled() flag
		PDECore.getDefault().getFeatureModelManager().targetReloaded();
	}

}