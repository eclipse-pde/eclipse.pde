/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class ProductOutlinePage extends FormOutlinePage {

	public ProductOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	public void sort(boolean sorting) {
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof DependenciesPage) {
			DependenciesPage page = (DependenciesPage) parent;
			IProduct product = ((IProductModel) page.getModel()).getProduct();
			if (product.useFeatures())
				return product.getFeatures();
			return product.getPlugins();
		}
		return new Object[0];
	}

	@Override
	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin)
			return DependenciesPage.PLUGIN_ID;
		if (item instanceof IProductFeature)
			return DependenciesPage.FEATURE_ID;
		return super.getParentPageId(item);
	}

}
