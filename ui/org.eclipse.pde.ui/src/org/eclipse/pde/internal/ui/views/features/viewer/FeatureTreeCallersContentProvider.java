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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;

public class FeatureTreeCallersContentProvider extends AbstractFeatureTreeContentProvider {

	public FeatureTreeCallersContentProvider(FeaturesViewInput featuresViewInput) {
		super(featuresViewInput);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFeatureModel) {
			IFeatureModel featureModel = (IFeatureModel) parentElement;
			String featureId = featureModel.getFeature().getId();

			Collection<IFeatureModel> features = fInput.getFeaturesViewInput().getIncludingFeatures(featureId);
			Collection<IProductModel> products = fInput.getFeaturesViewInput().getIncludingProducts(featureId);

			List<Object> all = new ArrayList<>(features.size() + products.size());
			all.addAll(features);
			all.addAll(products);

			return all.toArray();
		}

		return new Object[0];
	}

}
