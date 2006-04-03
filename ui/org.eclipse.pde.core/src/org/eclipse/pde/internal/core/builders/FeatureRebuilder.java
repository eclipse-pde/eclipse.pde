/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.internal.core.IFeatureModelDelta;
import org.eclipse.pde.internal.core.IFeatureModelListener;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.natures.PDE;

/**
 * Revalidates workspace features, on change in plug-ins or features
 */
public class FeatureRebuilder implements IFeatureModelListener,
		IPluginModelListener, Preferences.IPropertyChangeListener {
	private boolean fAutoBuilding;

	private boolean fLastJobPending = false;

	private Preferences fResourcesPreferences;

	public FeatureRebuilder() {
		super();
		fResourcesPreferences = ResourcesPlugin.getPlugin()
				.getPluginPreferences();
		fAutoBuilding = fResourcesPreferences
				.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING);
	}

	public void start() {
		PDECore.getDefault().getFeatureModelManager().addFeatureModelListener(
				this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		fResourcesPreferences.addPropertyChangeListener(this);

	}

	public void stop() {
		fResourcesPreferences.removePropertyChangeListener(this);
		PDECore.getDefault().getFeatureModelManager()
				.removeFeatureModelListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
	}

	public void modelsChanged(IFeatureModelDelta delta) {
		if ((IFeatureModelDelta.ADDED & delta.getKind()) != 0
				|| (IFeatureModelDelta.REMOVED & delta.getKind()) != 0)
			buildWorkspaceFeatures();
	}

	public void modelsChanged(PluginModelDelta delta) {
		if ((PluginModelDelta.ADDED & delta.getKind()) != 0
				|| (PluginModelDelta.REMOVED & delta.getKind()) != 0)
			buildWorkspaceFeatures();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (!event.getProperty().equals(ResourcesPlugin.PREF_AUTO_BUILDING))
			return;
		// get the new value of auto-build directly from the preferences
		boolean wasAutoBuilding = fAutoBuilding;
		fAutoBuilding = fResourcesPreferences
				.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING);
		// force a build if autobuild has been turned on
		if (!wasAutoBuilding && fAutoBuilding) {
			buildWorkspaceFeatures();
		}
	}

	private synchronized void buildWorkspaceFeatures() {
		if (fLastJobPending) {
			// previously scheduled job will do all features
			return;
		}
		if (!fAutoBuilding) {
			return;
		}
		Job buildJob = new Job(PDECoreMessages.FeatureConsistencyTrigger_JobName) { 
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
			 */
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_AUTO_BUILD == family;
			}

			//$NON-NLS-1$
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				if (!fAutoBuilding) {
					return Status.OK_STATUS;
				}
				try {
					IFeatureModel[] workspaceFeatures = PDECore.getDefault()
							.getWorkspaceModelManager().getFeatureModels();
					monitor.beginTask("", workspaceFeatures.length); //$NON-NLS-1$
					for (int i = 0; i < workspaceFeatures.length; i++) {
						IResource res = workspaceFeatures[i]
								.getUnderlyingResource();
						if (res == null) {
							monitor.worked(1);
							continue;
						}
						IProject projectToBuild = res.getProject();
						if (!projectToBuild.isOpen()) {
							monitor.worked(1);
							continue;
						}
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						try {
							if (projectToBuild.hasNature(PDE.FEATURE_NATURE)) {
								projectToBuild.build(
										IncrementalProjectBuilder.FULL_BUILD,
										PDE.FEATURE_BUILDER_ID, null,
										new SubProgressMonitor(monitor, 1));
							} else {
								monitor.worked(1);
							}
						} catch (CoreException e) {
						}

					}
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
				.buildRule());
		fLastJobPending = true;
		buildJob.addJobChangeListener(new JobChangeAdapter() {
			public void aboutToRun(IJobChangeEvent event) {
				super.aboutToRun(event);
				fLastJobPending = false;
				// don't listen to "done", other another job might have been
				// scheduled by then
				event.getJob().removeJobChangeListener(this);
			}

			public void done(IJobChangeEvent event) {
				super.done(event);
				// aboutToRun was not called, the job did not run
				fLastJobPending = false;
			}

		});
		buildJob.schedule(200);
	}

}
