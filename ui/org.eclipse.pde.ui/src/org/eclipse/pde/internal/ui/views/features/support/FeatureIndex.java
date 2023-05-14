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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.IFeatureModelDelta;
import org.eclipse.pde.internal.core.IFeatureModelListener;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.views.features.model.IProductModelListener;
import org.eclipse.pde.internal.ui.views.features.model.ProductModelManager;

public class FeatureIndex implements IFeatureModelListener, IProductModelListener {

	private final FeatureModelManager fFeatureModelManager;

	private final ProductModelManager fProductModelManager;

	private volatile Map<String, Collection<IFeatureModel>> fIncludingFeatures;

	private volatile Map<String, Collection<IProductModel>> fIncludingProducts;

	public FeatureIndex(FeatureModelManager featureModelManager, ProductModelManager productModelManager) {
		fFeatureModelManager = featureModelManager;
		fFeatureModelManager.addFeatureModelListener(this);
		fProductModelManager = productModelManager;
		fProductModelManager.addProductModelListener(this);
	}

	public boolean isInitialized() {
		return (fIncludingFeatures != null && fIncludingProducts != null);
	}

	public void ensureInitialized() {
		if (!isInitialized()) {
			reIndex();
		}
	}

	public Collection<IFeatureModel> getIncludingFeatures(String childId) {
		ensureInitialized();
		return fIncludingFeatures.getOrDefault(childId, Collections.emptySet());
	}

	public Collection<IProductModel> getIncludingProducts(String featureId) {
		ensureInitialized();
		return fIncludingProducts.getOrDefault(featureId, Collections.emptySet());
	}

	public void dispose() {
		fFeatureModelManager.removeFeatureModelListener(this);
		fProductModelManager.removeProductModelListener(this);
	}

	private void reIndex() {
		Map<String, Collection<IFeatureModel>> includingFeatures = new HashMap<>();
		Map<String, Collection<IProductModel>> includingProducts = new HashMap<>();

		for (IFeatureModel parentModel : fFeatureModelManager.getModels()) {
			for (IFeatureChild child : parentModel.getFeature().getIncludedFeatures()) {
				IFeatureModel childModel = fFeatureModelManager.findFeatureModel(child.getId());
				if (childModel != null) {
					index(includingFeatures, childModel, parentModel);
				}
			}
		}

		for (IProductModel productModel : fProductModelManager.getModels()) {
			for (IProductFeature productFeature : productModel.getProduct().getFeatures()) {
				IFeatureModel featureModel = fFeatureModelManager.findFeatureModel(productFeature.getId());
				if (featureModel != null) {
					index(includingProducts, featureModel, productModel);
				}
			}
		}

		fIncludingFeatures = includingFeatures;
		fIncludingProducts = includingProducts;
	}

	private void index(Map<String, Collection<IFeatureModel>> includingFeatures, IFeatureModel childModel,
			IFeatureModel parentModel) {
		String childId = childModel.getFeature().getId();

		Collection<IFeatureModel> parents = includingFeatures.computeIfAbsent(childId, key -> new HashSet<>());
		parents.add(parentModel);
	}

	private void index(Map<String, Collection<IProductModel>> includingProducts, IFeatureModel childModel,
			IProductModel productModel) {
		String featureId = childModel.getFeature().getId();

		Collection<IProductModel> products = includingProducts.computeIfAbsent(featureId, key -> new HashSet<>());
		products.add(productModel);
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		reIndex();
	}

	@Override
	public void modelsChanged() {
		reIndex();
	}

}
