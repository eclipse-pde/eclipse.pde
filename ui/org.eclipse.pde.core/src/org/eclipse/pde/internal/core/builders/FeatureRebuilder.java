/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.IFeatureModelDelta;
import org.eclipse.pde.internal.core.IFeatureModelListener;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Revalidates workspace features, on change in plug-ins or features
 */
public class FeatureRebuilder implements IFeatureModelListener, IPluginModelListener, IResourceChangeListener {

	private volatile boolean fTouchFeatures;

	public void start() {
		PDECore.getDefault().getFeatureModelManager().addFeatureModelListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		JavaCore.addPreProcessingResourceChangedListener(this, IResourceChangeEvent.PRE_BUILD);
	}

	public void stop() {
		Job.getJobManager().cancel(FeatureRebuilder.class);
		PDECore.getDefault().getFeatureModelManager().removeFeatureModelListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		if ((IFeatureModelDelta.ADDED & delta.getKind()) != 0 || (IFeatureModelDelta.REMOVED & delta.getKind()) != 0) {
			fTouchFeatures = true;
		}
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		if ((PluginModelDelta.ADDED & delta.getKind()) != 0 || (PluginModelDelta.REMOVED & delta.getKind()) != 0) {
			fTouchFeatures = true;
		} else {
			// listen for changes in checked/unchecked state
			// of plug-ins on the Target Platform preference page.
			// Only first entry will do, since workspace/target batch changes
			// typically do not mix.
			ModelEntry[] changed = delta.getChangedEntries();
			if (changed.length > 0) {
				if (!changed[0].hasWorkspaceModels()) {
					touchFeatures();
				}
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_BUILD && fTouchFeatures) {
			touchFeatures();
		}
	}

	private void touchFeatures() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] workspaceFeatures = manager.getWorkspaceModels();
		if (workspaceFeatures.length > 0) {
			IProgressMonitor monitor = new NullProgressMonitor();
			if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
				WorkspaceJob job = new WorkspaceJob("Touching Features") { //$NON-NLS-1$
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						SubMonitor subMonitor = SubMonitor.convert(monitor, workspaceFeatures.length);
						touch(workspaceFeatures, subMonitor);
						return Status.OK_STATUS;
					}

					@Override
					public boolean belongsTo(Object family) {
						return FeatureRebuilder.class == family;
					}
				};
				job.setUser(false);
				job.setSystem(true);
				job.setRule(getTouchRule(workspaceFeatures));
				job.schedule();
			} else {
				SubMonitor subMonitor = SubMonitor.convert(monitor, workspaceFeatures.length);
				touch(workspaceFeatures, subMonitor);
			}
		} else {
			fTouchFeatures = false;
		}
	}

	/**
	 * @return a rule for modifying the features or null if no valid resources
	 *         were found
	 */
	private ISchedulingRule getTouchRule(IFeatureModel[] workspaceFeatures) {
		List<IResource> nestedRules = new ArrayList<>(workspaceFeatures.length);
		for (int i = 0; i < workspaceFeatures.length; i++) {
			nestedRules.add(workspaceFeatures[i].getUnderlyingResource());
		}
		if (nestedRules.size() == 1) {
			return nestedRules.get(0);
		}
		return MultiRule.combine(nestedRules.toArray(new IResource[nestedRules.size()]));
	}

	private void touch(IFeatureModel[] workspaceFeatures, SubMonitor subMonitor) {
		for (IFeatureModel feature : workspaceFeatures) {
			if (subMonitor.isCanceled()) {
				return;
			}
			SubMonitor iterationMonitor = subMonitor.split(1);
			try {
				IResource resource = feature.getUnderlyingResource();
				if (resource != null && resource.isAccessible()) {
					resource.touch(iterationMonitor);
				}
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		fTouchFeatures = false;
	}

}
