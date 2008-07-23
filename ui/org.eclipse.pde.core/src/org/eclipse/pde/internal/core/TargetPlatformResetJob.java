/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pierre Carlson <mpcarl@us.ibm.com> - bug 233029
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class TargetPlatformResetJob extends Job {

	private PDEState fState;

	public TargetPlatformResetJob(PDEState newState) {
		super(PDECoreMessages.TargetPlatformResetJob_resetTarget);
		fState = newState;
		setRule(ResourcesPlugin.getWorkspace().getRoot());
	}

	protected IStatus run(IProgressMonitor monitor) {
		EclipseHomeInitializer.resetEclipseHomeVariable();
		PDECore.getDefault().getSourceLocationManager().reset();
		PDECore.getDefault().getJavadocLocationManager().reset();
		IPluginModelBase[] models = fState.getTargetModels();
		removeDisabledBundles(models);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.getExternalModelManager().setModels(models);
		// trigger Extension Registry reloaded before resetState call so listeners can update their extensions points accurately when target is reloaded
		PDECore.getDefault().getExtensionsRegistry().targetReloaded();
		manager.resetState(fState);
		PDECore.getDefault().getFeatureModelManager().targetReloaded();
		monitor.done();
		return Status.OK_STATUS;
	}

	private void removeDisabledBundles(IPluginModelBase[] models) {
		int number = models.length;
		for (int i = 0; i < models.length; i++) {
			if (!models[i].isEnabled()) {
				fState.removeBundleDescription(models[i].getBundleDescription());
				number -= 1;
			}
		}
		if (number < models.length)
			fState.resolveState(true);
	}

}
