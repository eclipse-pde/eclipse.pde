package org.eclipse.pde.internal.core.iproduct;


public interface IConfigurationFileInfo extends IProductObject {
	
	public static final String P_USE = "use"; //$NON-NLS-1$
	public static final String P_PATH = "path"; //$NON-NLS-1$
	
	void setUse(String use);
	
	String getUse();
	
	void setPath(String path);
	
	String getPath();

}
