/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rapicorp Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.ICSSInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationProperty;
import org.eclipse.pde.internal.core.iproduct.IIntroInfo;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.ILicenseInfo;
import org.eclipse.pde.internal.core.iproduct.IPluginConfiguration;
import org.eclipse.pde.internal.core.iproduct.IPreferencesInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.iproduct.IRepositoryInfo;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;

public class ProductModelFactory implements IProductModelFactory {

	private final IProductModel fModel;

	public ProductModelFactory(IProductModel model) {
		fModel = model;
	}

	@Override
	public IProduct createProduct() {
		return new Product(fModel);
	}

	@Override
	public IAboutInfo createAboutInfo() {
		return new AboutInfo(fModel);
	}

	@Override
	public IProductPlugin createPlugin() {
		return new ProductPlugin(fModel);
	}

	@Override
	public IPluginConfiguration createPluginConfiguration() {
		return new PluginConfiguration(fModel);
	}

	@Override
	public IConfigurationProperty createConfigurationProperty() {
		return new ConfigurationProperty(fModel);
	}

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
