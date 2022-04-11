/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 570760 - Option to automatically add requirements to product-launch
 *     Hannes Wellmann - Bug 325614 - Support mixed products (features and bundles)
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

import java.util.Locale;

public interface IProduct extends IProductObject {

	// see org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType
	public enum ProductType {
		BUNDLES, // only bundles are accepted in the product
		FEATURES, // only features are accepted in the product
		MIXED; // all kinds of installable units are accepted in the product

		public static ProductType parse(String s) {
			try {
				return ProductType.valueOf(s.toUpperCase(Locale.ENGLISH));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Illegal product type " + s, e); //$NON-NLS-1$
			}
		}

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	String P_ID = "id"; //$NON-NLS-1$
	String P_UID = "uid"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_APPLICATION = "application"; //$NON-NLS-1$
	String P_TYPE = "type"; //$NON-NLS-1$
	String P_INCLUDE_FRAGMENTS = "includeFragments"; //$NON-NLS-1$
	String P_INTRO_ID = "introId"; //$NON-NLS-1$
	String P_VERSION = "version"; //$NON-NLS-1$
	String P_INCLUDE_LAUNCHERS = "includeLaunchers"; //$NON-NLS-1$
	String P_INCLUDE_REQUIREMENTS_AUTOMATICALLY = "autoIncludeRequirements"; //$NON-NLS-1$

	String getId();

	String getProductId();

	String getName();

	String getApplication();

	String getVersion();

	String getDefiningPluginId();

	ProductType getType();

	boolean includeLaunchers();

	boolean includeRequirementsAutomatically();

	IAboutInfo getAboutInfo();

	IConfigurationFileInfo getConfigurationFileInfo();

	IArgumentsInfo getLauncherArguments();

	IJREInfo getJREInfo();

	IWindowImages getWindowImages();

	ISplashInfo getSplashInfo();

	IIntroInfo getIntroInfo();

	ILauncherInfo getLauncherInfo();

	ILicenseInfo getLicenseInfo();

	void addPlugins(IProductPlugin[] plugin);

	void addFeatures(IProductFeature[] feature);

	void addPluginConfigurations(IPluginConfiguration[] configurations);

	/**
	 * Adds the given properties to the list of properties known to this
	 * product.  Only properties that do not exist in the product configuration
	 * will be added.
	 *
	 * @param properties properties to add
	 */
	void addConfigurationProperties(IConfigurationProperty[] properties);

	void removePlugins(IProductPlugin[] plugins);

	void removeFeatures(IProductFeature[] feature);

	void removePluginConfigurations(IPluginConfiguration[] configurations);

	/**
	 * Removes the given properties from the list of properties known to this
	 * product.  If the properties are not in the product's properties, this
	 * method has no effect.
	 *
	 * @param properties properties to remove
	 */
	void removeConfigurationProperties(IConfigurationProperty[] properties);

	IPluginConfiguration findPluginConfiguration(String id);

	IProductPlugin[] getPlugins();

	IProductFeature[] getFeatures();

	IPluginConfiguration[] getPluginConfigurations();

	IRepositoryInfo[] getRepositories();

	void removeRepositories(IRepositoryInfo[] repositories);

	void addRepositories(IRepositoryInfo[] repositories);

	IPreferencesInfo getPreferencesInfo();

	ICSSInfo getCSSInfo();

	/**
	 * @return The list of properties set in the product configuration
	 */
	IConfigurationProperty[] getConfigurationProperties();

	void setId(String id);

	void setProductId(String id);

	void setVersion(String version);

	void setName(String name);

	void setAboutInfo(IAboutInfo info);

	void setApplication(String application);

	void setConfigurationFileInfo(IConfigurationFileInfo info);

	void setLauncherArguments(IArgumentsInfo info);

	void setJREInfo(IJREInfo info);

	void setWindowImages(IWindowImages images);

	void setSplashInfo(ISplashInfo info);

	void setIntroInfo(IIntroInfo introInfo);

	void setLauncherInfo(ILauncherInfo info);

	void setLicenseInfo(ILicenseInfo info);

	void setPreferencesInfo(IPreferencesInfo info);

	void setCSSInfo(ICSSInfo info);

	void setType(ProductType type);

	void setIncludeLaunchers(boolean exclude);

	void setIncludeRequirementsAutomatically(boolean includeRequirements);

	void reset();

	void swap(IProductFeature feature1, IProductFeature feature2);

	boolean containsPlugin(String id);

	boolean containsFeature(String id);

}
