/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.schema;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.neweditor.context.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SchemaInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public SchemaInputContextManager() {
	}

	public IBaseModel getAggregateModel() {
		return findSchema();
	}

	private IBaseModel findSchema() {
		InputContext scontext = findContext(SchemaInputContext.CONTEXT_ID);
		if (scontext!=null)
			return scontext.getModel();
		else
			return null;
	}
}