package org.eclipse.pde.internal.core.iproduct;


public interface IProductModelFactory {
	
	IProduct createProduct();
	
	IAboutInfo createAboutInfo();
	
	IProductPlugin createPlugin();

}
