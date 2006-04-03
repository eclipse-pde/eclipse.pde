/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.launcher.RuntimeWorkbenchShortcut;


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
	
	private IPluginModelBase[] getPlugins() {
		int lastDot = fId.lastIndexOf('.');
		if (lastDot == -1)
			return new IPluginModelBase[0];
		
		TreeMap map = new TreeMap();
		
		// add plugin declaring product and its pre-reqs
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(fId.substring(0, lastDot));
		if (model != null)
			RuntimeWorkbenchShortcut.addPluginAndDependencies(model, map);
		
		// add plugin declaring product application and its pre-reqs
		IPluginElement element = getProductExtension(fId);
		if (element != null) {
			IPluginAttribute attr = element.getAttribute("application"); //$NON-NLS-1$
			if (attr != null) {
				String appId = attr.getValue();
				lastDot = appId.lastIndexOf('.');
				if (lastDot != -1) {
					model = manager.findModel(appId.substring(0, lastDot));
					if (model != null) {
						RuntimeWorkbenchShortcut.addPluginAndDependencies(model, map);
					}
				}
			}
		}
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}

}
