package org.eclipse.pde.internal.core.iproduct;


public interface IProduct extends IProductObject {
	
	String getId();
	
	String getName();
	
	String getApplication();
	
	IAboutInfo getAboutInfo();
	
	void addPlugin(IProductPlugin plugin);
	
	void removePlugin(IProductPlugin plugin);
	
	IProductPlugin[] getPlugins();

	void setId(String id);
	
	void setName(String name);
	
	void setAboutInfo(IAboutInfo info);
	
	void setApplication(String application);
	
	void reset();
	
}
