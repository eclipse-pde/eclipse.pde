package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.editor.*;

public class ProductOutlinePage extends FormOutlinePage {

	public ProductOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	
	public void sort(boolean sorting) {
	}
	
	public void modelChanged(IModelChangedEvent event) {
		super.modelChanged(event);
	}
	
	protected Object[] getChildren(Object parent) {
		if (parent instanceof ConfigurationPage) {
			ConfigurationPage page = (ConfigurationPage)parent;
			IProduct product = ((IProductModel)page.getModel()).getProduct();
			if (product.useFeatures())
				return product.getFeatures();
			return product.getPlugins();
		}
		return new Object[0];
	}
	
	protected String getParentPageId(Object item) {
		if (item instanceof IProductPlugin)
			return ConfigurationPage.PLUGIN_ID;
		if (item instanceof IProductFeature)
			return ConfigurationPage.FEATURE_ID;
		return super.getParentPageId(item);
	}
	
}
