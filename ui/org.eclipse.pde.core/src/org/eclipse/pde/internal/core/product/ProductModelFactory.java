/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rapicorp Corporation - ongoing enhancements
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
	@Override
	public IProduct createProduct() {
		return new Product(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createAboutInfo()
	 */
	@Override
	public IAboutInfo createAboutInfo() {
		return new AboutInfo(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPlugin()
	 */
	@Override
	public IProductPlugin createPlugin() {
		return new ProductPlugin(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPluginConfiguration()
	 */
	@Override
	public IPluginConfiguration createPluginConfiguration() {
		return new PluginConfiguration(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPropertyConfiguration()
	 */
	@Override
	public IConfigurationProperty createConfigurationProperty() {
		return new ConfigurationProperty(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createConfigFileInfo()
	 */
	@Override
	public IConfigurationFileInfo createConfigFileInfo() {
		return new ConfigurationFileInfo(fModel);
	}

	@Override
	public IWindowImages createWindowImages() {
		return new WindowImages(fModel);
	}

	@Override
	public ISplashInfo createSplashInfo() {
		return new SplashInfo(fModel);
	}

	@Override
	public ILauncherInfo createLauncherInfo() {
		return new LauncherInfo(fModel);
	}

	@Override
	public IProductFeature createFeature() {
		return new ProductFeature(fModel);
	}

	@Override
	public IArgumentsInfo createLauncherArguments() {
		return new ArgumentsInfo(fModel);
	}

	@Override
	public IIntroInfo createIntroInfo() {
		return new IntroInfo(fModel);
	}

	@Override
	public IJREInfo createJVMInfo() {
		return new JREInfo(fModel);
	}

	@Override
	public ILicenseInfo createLicenseInfo() {
		return new LicenseInfo(fModel);
	}

	@Override
	public IRepositoryInfo createRepositoryInfo() {
		return new RepositoryInfo(fModel);
	}

	@Override
	public IPreferencesInfo createPreferencesInfo() {
		return new PreferencesInfo(fModel);
	}

	@Override
	public ICSSInfo createCSSInfo() {
		return new CSSInfo(fModel);
	}

}
