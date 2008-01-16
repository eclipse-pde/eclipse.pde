/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

/**
 * This operation generates a product configuration filling in fields based on information
 * stored a launch configuration. Product, application, JRE, and config information is 
 * collected from the launch config.
 */
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
			boolean useProduct = fLaunchConfiguration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false);
			if (useProduct) {
				String id = fLaunchConfiguration.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
				if (id != null) {
					initializeProductInfo(factory, product, id);
				}
			} else {
				String appName = fLaunchConfiguration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
				product.setApplication(appName);
			}

			// Set JRE info from information from the launch config
			String jreString = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
			if (jreString != null) {
				IPath jreContainerPath = new Path(jreString);
				IJREInfo jreInfo = product.getJREInfo();
				if (jreInfo == null) {
					jreInfo = product.getModel().getFactory().createJVMInfo();
				}
				jreInfo.setJREContainerPath(TargetPlatform.getOS(), jreContainerPath);
				product.setJREInfo(jreInfo);
			}

			addPlugins(factory, product, LaunchPluginValidator.getPluginList(fLaunchConfiguration));
			if (fLaunchConfiguration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
				super.initializeProduct(product);
			} else {
				String path = fLaunchConfiguration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, "/"); //$NON-NLS-1$
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

}
