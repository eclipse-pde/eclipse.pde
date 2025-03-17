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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureChildViewerFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (parentElement instanceof DeferredFeaturesViewInput input && element instanceof IFeatureModel featureModel) {
			boolean showProducts = input.getFeaturesViewInput().isIncludeProducts();

			String featureId = featureModel.getFeature().getId();
			boolean includedInFeature = !input.getFeaturesViewInput().getIncludingFeatures(featureId).isEmpty();
			boolean includedInProduct = !input.getFeaturesViewInput().getIncludingProducts(featureId).isEmpty();

			if (includedInFeature) {
				return false;
			} else if (showProducts && includedInProduct) {
				return false;
			}
		}

		return true;
	}

}
