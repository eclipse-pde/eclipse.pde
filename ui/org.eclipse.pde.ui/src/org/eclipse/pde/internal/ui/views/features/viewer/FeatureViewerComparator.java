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
package org.eclipse.pde.internal.ui.views.features.viewer;

import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.views.features.support.FeatureSupport;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;

public class FeatureViewerComparator extends ViewerComparator {

	private final FeatureSupport fFeatureSupport;

	public FeatureViewerComparator(FeaturesViewInput featuresViewInput) {
		fFeatureSupport = featuresViewInput.getFeatureSupport();
	}

	@Override
	public int category(Object element) {
		if (element instanceof IFeatureChild || element instanceof IProductFeature) {
			element = fFeatureSupport.toFeatureModel(element);
			if (element == null) {
				return 3;
			}
		}

		if (element instanceof IProductModel) {
			return 0;
		} else if (element instanceof IFeatureModel featureModel) {
			return (featureModel.isEditable() ? 1 : 2);
		} else if (element instanceof IFeaturePlugin || element instanceof IProductPlugin) {
			return 4;
		}

		return 5;
	}

}
