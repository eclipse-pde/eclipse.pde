package org.eclipse.pde.internal.core.iproduct;


public interface IProduct extends IProductObject {
	
	String getId();
	
	String getName();
	
	String getApplication();
	
	IAboutInfo getAboutInfo();
	
	boolean usesProduct();

	void setId(String id);
	
	void setName(String name);
	
	void setApplication(String application);
	
	void setUseProduct(boolean use);
	
	void reset();
	
}
