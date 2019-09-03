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
import org.eclipse.pde.internal.ui.views.features.support.FeatureIndex;
import org.eclipse.pde.internal.ui.views.features.support.FeatureInput;

public class FeatureChildViewerFilter extends ViewerFilter {

	private final FeatureIndex fFeatureIndex;

	public FeatureChildViewerFilter(FeatureIndex featureIndex) {
		fFeatureIndex = featureIndex;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (parentElement instanceof FeatureInput && element instanceof IFeatureModel) {
			IFeatureModel featureModel = (IFeatureModel) element;
			String featureId = featureModel.getFeature().getId();
			boolean includedInFeature = !fFeatureIndex.getIncludingFeatures(featureId).isEmpty();

			return !includedInFeature;
		}

		return true;
	}

}
