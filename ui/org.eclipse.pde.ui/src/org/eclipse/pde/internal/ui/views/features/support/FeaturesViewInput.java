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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductModel;

public class FeaturesViewInput {

	private final FeatureSupport fFeatureSupport;

	private final ProductSupport fProductSupport;

	private final PluginSupport fPluginSupport;

	private final FeatureIndex fIndex;

	private boolean fIncludeProducts;

	private boolean fIncludePlugins;

	public FeaturesViewInput() {
		fFeatureSupport = new FeatureSupport();
		fProductSupport = new ProductSupport();
		fPluginSupport = new PluginSupport();
		fIndex = new FeatureIndex(fFeatureSupport.getManager(), fProductSupport.getManager());
	}

	public IModel[] getModels() {
		IFeatureModel[] featureModels = getFeatures();
		IProductModel[] productModels = getProducts();

		List<IModel> all = new ArrayList<>(productModels.length + featureModels.length);
		all.addAll(Arrays.asList(productModels));
		all.addAll(Arrays.asList(featureModels));

		return all.toArray(new IModel[all.size()]);
	}

	private IFeatureModel[] getFeatures() {
		ensureInitialized();
		return fFeatureSupport.getManager().getModels();
	}

	private IProductModel[] getProducts() {
		ensureInitialized();
		return fProductSupport.getManager().getModels();
	}

	public Collection<IFeatureModel> getIncludingFeatures(String childId) {
		ensureInitialized();
		return fIndex.getIncludingFeatures(childId);
	}

	public Collection<IProductModel> getIncludingProducts(String featureId) {
		ensureInitialized();
		return fIndex.getIncludingProducts(featureId);
	}

	public boolean isInitialized() {
		return fFeatureSupport.getManager().isInitialized() && fProductSupport.getManager().isInitialized()
				&& fIndex.isInitialized();
	}

	private void ensureInitialized() {
		fIndex.ensureInitialized();
	}

	public boolean isIncludeProducts() {
		return fIncludeProducts;
	}

	public void setIncludeProducts(boolean includeProducts) {
		fIncludeProducts = includeProducts;
	}

	public boolean isIncludePlugins() {
		return fIncludePlugins;
	}

	public void setIncludePlugins(boolean includePlugins) {
		fIncludePlugins = includePlugins;
	}

	public FeatureSupport getFeatureSupport() {
		return fFeatureSupport;
	}

	public ProductSupport getProductSupport() {
		return fProductSupport;
	}

	public PluginSupport getPluginSupport() {
		return fPluginSupport;
	}

	public void dispose() {
		fIndex.dispose();
		fProductSupport.dispose();
	}

}
