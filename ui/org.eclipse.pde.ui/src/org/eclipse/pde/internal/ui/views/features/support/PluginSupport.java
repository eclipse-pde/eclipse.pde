/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.support;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;

public class PluginSupport {

	public IPluginModelBase toSinglePluginModel(IStructuredSelection selection) {
		if (selection.size() != 1) {
			return null;
		}

		Object firstElement = selection.getFirstElement();
		return toPluginModel(firstElement);
	}

	public IPluginModelBase toPluginModel(Object obj) {
		if (obj instanceof IPluginModelBase) {
			return (IPluginModelBase) obj;
		} else if (obj instanceof IFeaturePlugin featurePlugin) {
			return getManager().findModel(featurePlugin.getId());
		} else if (obj instanceof IProductPlugin productPlugin) {
			return getManager().findModel(productPlugin.getId());
		} else if (obj instanceof IProject) {
			return getManager().findModel((IProject) obj);
		} else if (obj instanceof IJavaProject) {
			return getManager().findModel(((IJavaProject) obj).getProject());
		} else {
			return null;
		}
	}

	public PluginModelManager getManager() {
		return PDECore.getDefault().getModelManager();
	}

}
