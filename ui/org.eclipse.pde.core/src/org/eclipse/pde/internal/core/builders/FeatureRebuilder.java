/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
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

	private boolean fTouchFeatures;

	public void start() {
		PDECore.getDefault().getFeatureModelManager().addFeatureModelListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		JavaCore.addPreProcessingResourceChangedListener(this, IResourceChangeEvent.PRE_BUILD);
	}

	public void stop() {
		PDECore.getDefault().getFeatureModelManager().removeFeatureModelListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
	}

	public void modelsChanged(IFeatureModelDelta delta) {
		if ((IFeatureModelDelta.ADDED & delta.getKind()) != 0 || (IFeatureModelDelta.REMOVED & delta.getKind()) != 0)
			fTouchFeatures = true;
	}

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
				if (!changed[0].hasWorkspaceModels())
					touchFeatures();
			}
		}
	}

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
			monitor.beginTask("", workspaceFeatures.length); //$NON-NLS-1$
			for (int i = 0; i < workspaceFeatures.length; i++) {
				try {
					IResource resource = workspaceFeatures[i].getUnderlyingResource();
					if (resource != null) {
						resource.touch(new SubProgressMonitor(monitor, 1));
					} else {
						monitor.worked(1);
					}
				} catch (CoreException e) {
				}
			}
		}
		fTouchFeatures = false;
	}

}
