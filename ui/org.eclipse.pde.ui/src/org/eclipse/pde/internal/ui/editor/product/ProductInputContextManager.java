package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;


public class ProductInputContextManager extends InputContextManager {

	/**
	 * @param editor
	 */
	public ProductInputContextManager(PDEFormEditor editor) {
		super(editor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContextManager#getAggregateModel()
	 */
	public IBaseModel getAggregateModel() {
		InputContext context = findContext(ProductInputContext.CONTEXT_ID);
		return (context != null) ? context.getModel() : null;
	}

}
