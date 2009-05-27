/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.swt.widgets.Shell;

public class CreateFeatureProjectOperation extends AbstractCreateFeatureOperation {

	protected IPluginBase[] fPlugins;

	public CreateFeatureProjectOperation(IProject project, IPath location, FeatureData featureData, IPluginBase[] plugins, Shell shell) {
		super(project, location, featureData, shell);
		fPlugins = plugins != null ? plugins : new IPluginBase[0];
	}

	protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
		IFeaturePlugin[] added = new IFeaturePlugin[fPlugins.length];
		for (int i = 0; i < fPlugins.length; i++) {
			IPluginBase plugin = fPlugins[i];
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(plugin);
			fplugin.setVersion("0.0.0"); //$NON-NLS-1$
			fplugin.setUnpack(CoreUtility.guessUnpack(plugin.getPluginModel().getBundleDescription()));
			added[i] = fplugin;
		}
		feature.addPlugins(added);
	}

}
