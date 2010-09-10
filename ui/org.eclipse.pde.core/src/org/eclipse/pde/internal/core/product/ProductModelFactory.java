/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import org.eclipse.pde.internal.core.iproduct.*;

public class ProductModelFactory implements IProductModelFactory {

	private IProductModel fModel;

	public ProductModelFactory(IProductModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createProduct()
	 */
	public IProduct createProduct() {
		return new Product(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createAboutInfo()
	 */
	public IAboutInfo createAboutInfo() {
		return new AboutInfo(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPlugin()
	 */
	public IProductPlugin createPlugin() {
		return new ProductPlugin(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPluginConfiguration()
	 */
	public IPluginConfiguration createPluginConfiguration() {
		return new PluginConfiguration(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPropertyConfiguration()
	 */
	public IConfigurationProperty createConfigurationProperty() {
		return new ConfigurationProperty(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createConfigFileInfo()
	 */
	public IConfigurationFileInfo createConfigFileInfo() {
		return new ConfigurationFileInfo(fModel);
	}

	public IWindowImages createWindowImages() {
		return new WindowImages(fModel);
	}

	public ISplashInfo createSplashInfo() {
		return new SplashInfo(fModel);
	}

	public ILauncherInfo createLauncherInfo() {
		return new LauncherInfo(fModel);
	}

	public IProductFeature createFeature() {
		return new ProductFeature(fModel);
	}

	public IArgumentsInfo createLauncherArguments() {
		return new ArgumentsInfo(fModel);
	}

	public IIntroInfo createIntroInfo() {
		return new IntroInfo(fModel);
	}

	public IJREInfo createJVMInfo() {
		return new JREInfo(fModel);
	}

	public ILicenseInfo createLicenseInfo() {
		return new LicenseInfo(fModel);
	}

}
