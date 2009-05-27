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
package org.eclipse.pde.internal.ui.wizards.product;

import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyCalculator;

public class ProductFromExtensionOperation extends BaseProductCreationOperation {

	private String fId;

	public ProductFromExtensionOperation(IFile file, String productId) {
		super(file);
		fId = productId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.product.BaseProductCreationOperation#initializeProduct(org.eclipse.pde.internal.core.iproduct.IProduct)
	 */
	protected void initializeProduct(IProduct product) {
		if (fId == null)
			return;
		IProductModelFactory factory = product.getModel().getFactory();
		initializeProductInfo(factory, product, fId);
		addPlugins(factory, product, getPlugins());
		super.initializeProduct(product);
	}

	private String[] getPlugins() {
		int lastDot = fId.lastIndexOf('.');
		if (lastDot == -1)
			return new String[0];

		DependencyCalculator calculator = new DependencyCalculator(false);
		// add plugin declaring product and its pre-reqs
		IPluginModelBase model = PluginRegistry.findModel(fId.substring(0, lastDot));
		if (model != null)
			calculator.findDependency(model);

		// add plugin declaring product application and its pre-reqs
		IPluginElement element = getProductExtension(fId);
		if (element != null) {
			IPluginAttribute attr = element.getAttribute("application"); //$NON-NLS-1$
			if (attr != null) {
				String appId = attr.getValue();
				lastDot = appId.lastIndexOf('.');
				if (lastDot != -1) {
					model = PluginRegistry.findModel(appId.substring(0, lastDot));
					if (model != null) {
						calculator.findDependency(model);
					}
				}
			}
		}
		Set ids = calculator.getBundleIDs();
		return (String[]) ids.toArray(new String[ids.size()]);
	}

}
