/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.feature;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.neweditor.context.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FeatureInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public FeatureInputContextManager() {
	}

	public IBaseModel getAggregateModel() {
		return findFeatureModel();
	}

	private IBaseModel findFeatureModel() {
		InputContext fcontext = findContext(FeatureInputContext.CONTEXT_ID);
		if (fcontext!=null)
			return fcontext.getModel();
		else
			return null;
	}
}