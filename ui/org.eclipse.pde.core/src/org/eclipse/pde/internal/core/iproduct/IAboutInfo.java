package org.eclipse.pde.internal.core.iproduct;


public interface IAboutInfo extends IProductObject {
	
	public static final String P_IMAGE = "image"; //$NON-NLS-1$
	public static final String P_TEXT = "text"; //$NON-NLS-1$
	
	void setText(String text);
	
	String getText();
	
	void setImagePath(String path);
	
	String getImagePath();

}
