package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.swt.widgets.*;

public class SynchronizationOperation extends ProductDefinitionOperation {

	public SynchronizationOperation(IProduct product, Shell shell) {
		super(product, getPluginId(product), getProductId(product), product.getApplication(), shell);
	}
	
	private static String getProductId(IProduct product) {
		String full = product.getId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(index + 1) : full;
	}
	
	private static String getPluginId(IProduct product) {
		String full = product.getId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(0, index) : full;
	}

}
