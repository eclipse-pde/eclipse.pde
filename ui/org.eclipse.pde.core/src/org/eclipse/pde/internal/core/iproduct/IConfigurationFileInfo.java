package org.eclipse.pde.internal.core.iproduct;


public interface IConfigurationFileInfo extends IProductObject {
	
	public final static int USE_DEFAULT = 0;
	public final static int USE_WORKSPACE = 1;
	public final static int USE_FILESYSTEM = 2;
	
	void setUse(int use);
	
	int getUse();
	
	void setPath(String path);
	
	String getPath();

}
