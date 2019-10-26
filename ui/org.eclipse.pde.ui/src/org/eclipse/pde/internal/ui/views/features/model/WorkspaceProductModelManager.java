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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;

public class WorkspaceProductModelManager extends WorkspaceModelManager<Collection<IProductModel>> {

	public final static String PRODUCT_FILENAME_SUFFIX = ".product"; //$NON-NLS-1$

	@Override
	protected boolean isInterestingProject(IProject project) {
		return (project.isOpen() && !findProductFiles(project, false).isEmpty());
	}

	@Override
	protected boolean isInterestingFolder(IFolder folder) {
		return true;
	}

	@Override
	protected void createModel(IProject project, boolean notify) {
		for (IFile product : findProductFiles(project, true)) {
			createSingleModel(project, product, notify);
		}
	}

	private void createSingleModel(IProject project, IFile product, boolean notify) {
		IProductModel model = new WorkspaceProductModel(product, true);
		loadModel(model, false);

		Collection<IProductModel> models = getModelsMap().computeIfAbsent(project, key -> new ArrayList<>());
		models.add(model);

		if (notify) {
			addChange(model, IModelProviderEvent.MODELS_ADDED);
		}
	}

	@Override
	protected Collection<IProductModel> removeModel(IProject project) {
		Collection<IProductModel> models = getModelsMap().remove(project);
		if (models != null) {
			for (IProductModel model : models) {
				addChange(model, IModelProviderEvent.MODELS_REMOVED);
			}
		}
		return models;
	}

	private Collection<IProductModel> removeSingleModel(IProject project, IProductModel model) {
		Collection<IProductModel> models = getModelsMap().get(project);
		if (models != null) {
			if (models.remove(model)) {
				addChange(model, IModelProviderEvent.MODELS_REMOVED);
			}
			if (models.isEmpty()) {
				getModelsMap().remove(project);
			}
		}
		return models;
	}

	private IProductModel getSingleModel(IFile productFile) {
		Collection<IProductModel> models = getModel(productFile.getProject());
		if (models != null) {
			for (IProductModel model : models) {
				if (model.getUnderlyingResource().equals(productFile)) {
					return model;
				}
			}
		}
		return null;
	}

	@Override
	protected void handleFileDelta(IResourceDelta delta) {
		if (isProductFile(delta.getResource())) {
			IFile file = (IFile) delta.getResource();
			IProject project = file.getProject();

			IProductModel model = getSingleModel(file);
			int kind = delta.getKind();
			if (kind == IResourceDelta.REMOVED && model != null) {
				removeSingleModel(project, model);
			} else if (kind == IResourceDelta.ADDED && model == null) {
				createSingleModel(project, file, true);
			} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				loadModel(model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		}
	}

	@Override
	protected void addListeners() {
		int event = IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE;
		PDECore.getWorkspace().addResourceChangeListener(this, event);
	}

	@Override
	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		super.removeListeners();
	}

	protected IProductModel[] getProductModels() {
		initialize();

		Collection<IProductModel> flattenedModels = new ArrayList<>();
		for (Collection<IProductModel> models : getModelsMap().values()) {
			flattenedModels.addAll(models);
		}

		return flattenedModels.toArray(new IProductModel[flattenedModels.size()]);
	}

	private static boolean isProductFile(IResourceProxy proxy) {
		return proxy.getType() == IResource.FILE && proxy.getName().endsWith(PRODUCT_FILENAME_SUFFIX);
	}

	private static boolean isProductFile(IResource resource) {
		return resource.getType() == IResource.FILE && resource.getName().endsWith(PRODUCT_FILENAME_SUFFIX);
	}

	private static Collection<IFile> findProductFiles(IContainer container, boolean findAll) {
		try {
			Collection<IFile> products = new ArrayList<>();

			container.accept(proxy -> {
				if (isProductFile(proxy) && (findAll || products.isEmpty())) {
					products.add((IFile) proxy.requestResource());
				}
				return (findAll || products.isEmpty());
			}, IResource.NONE);

			return products;
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

}
