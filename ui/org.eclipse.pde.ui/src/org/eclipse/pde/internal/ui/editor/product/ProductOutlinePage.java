/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	public void sort(boolean sorting) {
	}

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

	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin)
			return DependenciesPage.PLUGIN_ID;
		if (item instanceof IProductFeature)
			return DependenciesPage.FEATURE_ID;
		return super.getParentPageId(item);
	}

}
