package org.eclipse.pde.internal.core.iproduct;


public interface IAboutInfo extends IProductObject {
	
	public static final String P_IMAGE = "image";
	public static final String P_TEXT = "text";
	
	void setText(String text);
	
	String getText();
	
	void setImagePath(String path);
	
	String getImagePath();

}
