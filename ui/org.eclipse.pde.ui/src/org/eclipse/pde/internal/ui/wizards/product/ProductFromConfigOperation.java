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
package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.ILauncherSettings;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;


public class ProductFromConfigOperation extends BaseProductCreationOperation {

	private ILaunchConfiguration fLaunchConfiguration;

	public ProductFromConfigOperation(IFile file, ILaunchConfiguration config) {
		super(file);
		fLaunchConfiguration = config;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.product.BaseProductCreationOperation#initializeProduct(org.eclipse.pde.internal.core.iproduct.IProduct)
	 */
	protected void initializeProduct(IProduct product) {
		if (fLaunchConfiguration == null)
			return;
		try {
			IProductModelFactory factory = product.getModel().getFactory();
			boolean useProduct = fLaunchConfiguration.getAttribute(ILauncherSettings.USE_PRODUCT, false);
			if (useProduct) {
				String id = fLaunchConfiguration.getAttribute(ILauncherSettings.PRODUCT, (String)null);
				if (id != null) {
					initializeProductInfo(factory, product, id);
				}
			} else {
				String appName = fLaunchConfiguration.getAttribute(ILauncherSettings.APPLICATION, LauncherUtils.getDefaultApplicationName());
				product.setApplication(appName);
			}
			addPlugins(factory, product, getSelectedPlugins());
			if (fLaunchConfiguration.getAttribute(ILauncherSettings.CONFIG_GENERATE_DEFAULT, true)) {
				super.initializeProduct(product);
			} else {
				String path = fLaunchConfiguration.getAttribute(ILauncherSettings.CONFIG_TEMPLATE_LOCATION, "/"); //$NON-NLS-1$
				IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(path));
				if (container != null) {
					IConfigurationFileInfo info = factory.createConfigFileInfo();
					info.setUse("custom"); //$NON-NLS-1$
					info.setPath(container.getFullPath().toString());
					product.setConfigurationFileInfo(info);	
				} else {
					super.initializeProduct(product);
				}
			}
		} catch (CoreException e) {
		}	
	}
	
	private IPluginModelBase[] getSelectedPlugins() {
		try {
			if (fLaunchConfiguration.getAttribute(ILauncherSettings.USE_DEFAULT, true)) {
				return PDECore.getDefault().getModelManager().getPlugins();
			}
			return LauncherUtils.getSelectedPlugins(fLaunchConfiguration);
		} catch (CoreException e) {
		}
		return new IPluginModelBase[0];
	}

}
