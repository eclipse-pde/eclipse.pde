package org.eclipse.pde.internal.core.iproduct;

public interface IWindowImages extends IProductObject {
	
	public static final String P_LARGE = "large"; //$NON-NLS-1$
	public static final String P_SMALL = "small"; //$NON-NLS-1$
	
	String getLargeImagePath();
	String getSmallImagePath();
	
	void setLargeImagePath(String path);
	void setSmallImagePath(String path);

}
