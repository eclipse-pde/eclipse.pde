package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.swt.widgets.*;

public class SynchronizationOperation extends ProductDefinitionOperation {

	public SynchronizationOperation(IProduct product, String pluginId,
			String productId, String application, Shell shell) {
		super(product, null, product.getId(), product.getApplication(), shell);
	}

}
