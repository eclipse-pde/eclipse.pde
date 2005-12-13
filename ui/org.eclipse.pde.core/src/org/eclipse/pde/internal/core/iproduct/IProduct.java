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
package org.eclipse.pde.internal.core.iproduct;


public interface IProduct extends IProductObject {
	
	String P_ID = "id"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_APPLICATION = "application"; //$NON-NLS-1$
	String P_USEFEATURES = "useFeatures"; //$NON-NLS-1$
	String P_INCLUDE_FRAGMENTS = "includeFragments"; //$NON-NLS-1$
	String P_INTRO_ID = "introId"; //$NON-NLS-1$
	
	String getId();
	
	String getName();
	
	String getApplication();
	
	String getDefiningPluginId();
	
	boolean useFeatures();
	
	IAboutInfo getAboutInfo();
	
	IConfigurationFileInfo getConfigurationFileInfo();
	
	IArgumentsInfo getLauncherArguments();
	
	IWindowImages getWindowImages();
	
	ISplashInfo getSplashInfo();
	
	IIntroInfo getIntroInfo();
	
	ILauncherInfo getLauncherInfo();
	
	void addPlugin(IProductPlugin plugin);
	
	void addFeature(IProductFeature feature);
	
	void removePlugin(IProductPlugin plugin);
	
	void removeFeature(IProductFeature feature);
	
	IProductPlugin[] getPlugins();
	
	IProductFeature[] getFeatures();

	void setId(String id);
	
	void setName(String name);
	
	void setAboutInfo(IAboutInfo info);
	
	void setApplication(String application);
	
	void setConfigurationFileInfo(IConfigurationFileInfo info);
	
	void setLauncherArguments(IArgumentsInfo info);
	
	void setWindowImages(IWindowImages images);
	
	void setSplashInfo(ISplashInfo info);
	
	void setIntroInfo(IIntroInfo introInfo);
	
	void setLauncherInfo(ILauncherInfo info);
	
	void setUseFeatures(boolean use);
	
	void reset();
	
	boolean containsPlugin(String id);
	
	boolean containsFeature(String id);
	
}
