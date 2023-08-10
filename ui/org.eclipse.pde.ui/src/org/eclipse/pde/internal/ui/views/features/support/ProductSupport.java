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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.views.features.model.ProductModelManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class ProductSupport {

	private final ProductModelManager fManager;

	public ProductSupport() {
		fManager = new ProductModelManager();
	}

	public void openProductEditor(IProductModel productModel) {
		IResource resource = productModel.getUnderlyingResource();
		try {
			IEditorInput input = null;
			if (resource != null) {
				input = new FileEditorInput((IFile) resource);
				IDE.openEditor(PDEPlugin.getActivePage(), input, IPDEUIConstants.PRODUCT_EDITOR_ID, true);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	public IProductModel toProductModel(Object obj) {
		if (obj instanceof IProduct product) {
			return getManager().findProductModel(product.getId());
		} else if (obj instanceof IProductModel) {
			return (IProductModel) obj;
		} else {
			return null;
		}
	}

	public ProductModelManager getManager() {
		return fManager;
	}

	public void dispose() {
		fManager.shutdown();
	}

}
