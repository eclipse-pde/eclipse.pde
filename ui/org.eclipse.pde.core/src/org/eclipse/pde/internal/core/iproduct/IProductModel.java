package org.eclipse.pde.internal.core.iproduct;

import org.eclipse.pde.core.*;


public interface IProductModel extends IModel, IModelChangeProvider {
	
	IProduct getProduct();
	
	IProductModelFactory getFactory();
	
	String getInstallLocation();
	
	boolean isEnabled();
	
	void setEnabled(boolean enabled);

}
