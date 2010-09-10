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
package org.eclipse.pde.internal.core.iproduct;

public interface IProduct extends IProductObject {

	String P_ID = "id"; //$NON-NLS-1$
	String P_UID = "uid"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_APPLICATION = "application"; //$NON-NLS-1$
	String P_USEFEATURES = "useFeatures"; //$NON-NLS-1$
	String P_INCLUDE_FRAGMENTS = "includeFragments"; //$NON-NLS-1$
	String P_INTRO_ID = "introId"; //$NON-NLS-1$
	String P_VERSION = "version"; //$NON-NLS-1$
	String P_INCLUDE_LAUNCHERS = "includeLaunchers"; //$NON-NLS-1$

	String getId();

	String getProductId();

	String getName();

	String getApplication();

	String getVersion();

	String getDefiningPluginId();

	boolean useFeatures();

	boolean includeLaunchers();

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

	void setUseFeatures(boolean use);

	void setIncludeLaunchers(boolean exclude);

	void reset();

	void swap(IProductFeature feature1, IProductFeature feature2);

	boolean containsPlugin(String id);

	boolean containsFeature(String id);

}
