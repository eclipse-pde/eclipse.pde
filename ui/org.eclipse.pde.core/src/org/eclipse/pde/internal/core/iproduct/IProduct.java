package org.eclipse.pde.internal.core.iproduct;


public interface IProduct extends IProductObject {
	
	String P_ID = "id"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_APPLICATION = "application"; //$NON-NLS-1$
	String P_USEFEATURES = "useFeatures"; //$NON-NLS-1$
	String P_DESTINATION = "destination"; //$NON-NLS-1$
	String P_INCLUDE_FRAGMENTS = "includeFragments"; //$NON-NLS-1$
	
	String getId();
	
	String getName();
	
	String getApplication();
	
	boolean useFeatures();
	
	boolean includeFragments();
	
	IAboutInfo getAboutInfo();
	
	String getExportDestination();
	
	IConfigurationFileInfo getConfigurationFileInfo();
	
	IWindowImages getWindowImages();
	
	ISplashInfo getSplashInfo();
	
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
	
	void setWindowImages(IWindowImages images);
	
	void setSplashInfo(ISplashInfo info);
	
	void setLauncherInfo(ILauncherInfo info);
	
	void setExportDestination(String destination);
	
	void setUseFeatures(boolean use);
	
	void setIncludeFragments(boolean include);
	
	void reset();
	
	boolean containsPlugin(String id);
	
	boolean containsFeature(String id);
	
}
