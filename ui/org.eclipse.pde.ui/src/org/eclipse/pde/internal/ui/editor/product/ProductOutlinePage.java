/*******************************************************************************
 *  Copyright (c) 2005, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class ProductOutlinePage extends FormOutlinePage {

	public ProductOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof DependenciesPage page && page.getModel() instanceof IProductModel model) {
			IProduct product = model.getProduct();
			return switch (product.getType())
				{
				case BUNDLES -> product.getPlugins();
				case FEATURES -> product.getFeatures();
				case MIXED -> Stream.of(product.getFeatures(), product.getPlugins()).flatMap(Arrays::stream).toArray();
				};
		}
		return new Object[0];
	}

	@Override
	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin) {
			return DependenciesPage.PLUGIN_ID;
		}
		if (item instanceof IProductFeature) {
			return DependenciesPage.FEATURE_ID;
		}
		return super.getParentPageId(item);
	}

}
