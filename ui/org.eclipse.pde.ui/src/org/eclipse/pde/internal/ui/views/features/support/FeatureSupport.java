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
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureSupport {

	private FeatureSupport() {
	}

	public static IFeatureModel toFeatureModel(Object obj) {
		if (obj instanceof IFeatureModel) {
			return (IFeatureModel) obj;
		} else if (obj instanceof IIdentifiable) {
			IIdentifiable featureOrChild = (IIdentifiable) obj;
			return getManager().findFeatureModel(featureOrChild.getId());
		} else if (obj instanceof IProject) {
			return getManager().getFeatureModel((IProject) obj);
		} else {
			return null;
		}
	}

	public static FeatureModelManager getManager() {
		return PDECore.getDefault().getFeatureModelManager();
	}

}
