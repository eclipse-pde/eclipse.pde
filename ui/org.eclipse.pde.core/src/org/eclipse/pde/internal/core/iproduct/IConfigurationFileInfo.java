package org.eclipse.pde.internal.core.iproduct;


public interface IConfigurationFileInfo extends IProductObject {
	
	void setUse(String use);
	
	String getUse();
	
	void setPath(String path);
	
	String getPath();

}
