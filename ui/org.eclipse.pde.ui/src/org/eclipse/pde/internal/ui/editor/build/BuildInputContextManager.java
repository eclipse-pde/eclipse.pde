/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.context.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BuildInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public BuildInputContextManager() {
	}

	public IBaseModel getAggregateModel() {
		return findBuildModel();
	}

	private IBaseModel findBuildModel() {
		InputContext bcontext = findContext(BuildInputContext.CONTEXT_ID);
		if (bcontext!=null)
			return bcontext.getModel();
		else
			return null;
	}
}