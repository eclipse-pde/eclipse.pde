package org.eclipse.pde.internal.core.iproduct;


public interface IProduct extends IProductObject {
	
	String P_ID = "id"; //$NON-NLS-1$
	String P_NAME = "name"; //$NON-NLS-1$
	String P_APPLICATION = "application"; //$NON-NLS-1$
	String P_USEFEATURES = "useFeatures";
	String P_DESTINATION = "destination";
	String P_INCLUDE_FRAGMENTS = "includeFragments";
	
	String getId();
	
	String getName();
	
	String getApplication();
	
	boolean useFeatures();
	
	boolean includeFragments();
	
	IAboutInfo getAboutInfo();
	
	String getExportDestination();
	
	IConfigurationFileInfo getConfigurationFileInfo();
	
	void addPlugin(IProductPlugin plugin);
	
	void removePlugin(IProductPlugin plugin);
	
	IProductPlugin[] getPlugins();

	void setId(String id);
	
	void setName(String name);
	
	void setAboutInfo(IAboutInfo info);
	
	void setApplication(String application);
	
	void setConfigurationFileInfo(IConfigurationFileInfo info);
	
	void setExportDestination(String destination);
	
	void setUseFeatures(boolean use);
	
	void setIncludeFragments(boolean include);
	
	void reset();
	
	boolean containsPlugin(String id);
	
}
