package org.eclipse.pde.internal.core.iproduct;

public interface IWindowImages extends IProductObject {
	
	public static final String P_LARGE = "large";
	public static final String P_SMALL = "small";
	
	String getLargeImagePath();
	String getSmallImagePath();
	
	void setLargeImagePath(String path);
	void setSmallImagePath(String path);

}
