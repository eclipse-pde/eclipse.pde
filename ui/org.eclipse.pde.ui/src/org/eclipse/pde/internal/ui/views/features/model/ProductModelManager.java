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
package org.eclipse.pde.internal.ui.views.features.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.iproduct.IProductModel;

public class ProductModelManager {

	private final Collection<IProductModelListener> fListeners = new ArrayList<>();

	private final WorkspaceProductModelManager fWorkspaceProductModelManager;

	private Map<String, IProductModel> fProductModels;

	public ProductModelManager() {
		fWorkspaceProductModelManager = new WorkspaceProductModelManager();
		fWorkspaceProductModelManager.addModelProviderListener(this::handleModelProviderChange);
	}

	private void handleModelProviderChange(IModelProviderEvent event) {
		ensureInitialized();

		for (IModel model : event.getRemovedModels()) {
			IProductModel productModel = (IProductModel) model;
			fProductModels.remove(productModel.getProduct().getId());
		}
		for (IModel model : event.getAddedModels()) {
			IProductModel productModel = (IProductModel) model;
			fProductModels.put(productModel.getProduct().getId(), productModel);
		}
		for (IModel model : event.getChangedModels()) {
			IProductModel productModel = (IProductModel) model;
			fProductModels.put(productModel.getProduct().getId(), productModel);
		}
		for (IProductModelListener listener : fListeners) {
			listener.modelsChanged();
		}
	}

	public boolean isInitialized() {
		return (fProductModels != null);
	}

	private void ensureInitialized() {
		if (fProductModels == null) {
			fProductModels = new HashMap<>();
			for (IProductModel productModel : fWorkspaceProductModelManager.getProductModels()) {
				fProductModels.put(productModel.getProduct().getId(), productModel);
			}
		}
	}

	public IProductModel findProductModel(String id) {
		ensureInitialized();
		return fProductModels.get(id);
	}

	public IProductModel[] getModels() {
		ensureInitialized();
		return fWorkspaceProductModelManager.getProductModels();
	}

	public void addProductModelListener(IProductModelListener listener) {
		fListeners.add(listener);
	}

	public void removeProductModelListener(IProductModelListener listener) {
		fListeners.remove(listener);
	}

	public synchronized void shutdown() {
		fWorkspaceProductModelManager.shutdown();
	}

}
