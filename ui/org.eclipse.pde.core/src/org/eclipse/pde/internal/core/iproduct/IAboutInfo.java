package org.eclipse.pde.internal.core.iproduct;


public interface IAboutInfo extends IProductObject {
	
	void setText(String text);
	
	String getText();
	
	void setImagePath(String path);
	
	String getImagePath();

}
