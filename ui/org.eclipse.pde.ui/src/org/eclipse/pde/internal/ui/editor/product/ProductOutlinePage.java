/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.IModelChangedEvent;
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
	
	public void sort(boolean sorting) {
	}
	
	public void modelChanged(IModelChangedEvent event) {
		super.modelChanged(event);
	}
	
	protected Object[] getChildren(Object parent) {
		if (parent instanceof ConfigurationPage) {
			ConfigurationPage page = (ConfigurationPage)parent;
			IProduct product = ((IProductModel)page.getModel()).getProduct();
			if (product.useFeatures())
				return product.getFeatures();
			return product.getPlugins();
		}
		return new Object[0];
	}
	
	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin)
			return ConfigurationPage.PLUGIN_ID;
		if (item instanceof IProductFeature)
			return ConfigurationPage.FEATURE_ID;
		return super.getParentPageId(item);
	}
	
}
