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
 *     Code 9 Corporation - ongoing enhancements
 *     Rapicorp Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface IProductModelFactory {

	IProduct createProduct();

	IAboutInfo createAboutInfo();

	IProductPlugin createPlugin();

	IPluginConfiguration createPluginConfiguration();

	IConfigurationProperty createConfigurationProperty();

	IProductFeature createFeature();

	IConfigurationFileInfo createConfigFileInfo();

	IWindowImages createWindowImages();

	ISplashInfo createSplashInfo();

	ILauncherInfo createLauncherInfo();

	IArgumentsInfo createLauncherArguments();

	IIntroInfo createIntroInfo();

	IJREInfo createJVMInfo();

	ILicenseInfo createLicenseInfo();

	IRepositoryInfo createRepositoryInfo();

	IPreferencesInfo createPreferencesInfo();

	ICSSInfo createCSSInfo();

}
