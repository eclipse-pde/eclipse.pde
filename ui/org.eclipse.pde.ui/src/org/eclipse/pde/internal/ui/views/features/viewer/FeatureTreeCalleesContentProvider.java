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
import java.util.Arrays;
import java.util.List;

import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;

public class FeatureTreeCalleesContentProvider extends AbstractFeatureTreeContentProvider {

	public FeatureTreeCalleesContentProvider(FeaturesViewInput featuresViewInput) {
		super(featuresViewInput);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProductModel productModel) {
			Object[] features = productModel.getProduct().getFeatures();
			Object[] plugins = productModel.getProduct().getPlugins();

			List<Object> all = new ArrayList<>(features.length + plugins.length);
			all.addAll(Arrays.asList(features));
			all.addAll(Arrays.asList(plugins));

			return all.toArray();
		} else if (parentElement instanceof IFeatureModel featureModel) {
			Object[] features = featureModel.getFeature().getIncludedFeatures();
			Object[] plugins = featureModel.getFeature().getPlugins();

			List<Object> all = new ArrayList<>(features.length + plugins.length);
			all.addAll(Arrays.asList(features));
			all.addAll(Arrays.asList(plugins));

			return all.toArray();
		} else if (parentElement instanceof IFeatureChild featureChild) {
			IFeatureModel featureModel = fFeatureModelManager.findFeatureModel(featureChild.getId());
			return getChildren(featureModel);
		} else if (parentElement instanceof IProductFeature productFeature) {
			IFeatureModel featureModel = fFeatureModelManager.findFeatureModel(productFeature.getId());
			return getChildren(featureModel);
		}

		return new Object[0];
	}

}
