/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.site;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.neweditor.context.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SiteInputContextManager extends InputContextManager {
	/**
	 * 
	 */
	public SiteInputContextManager() {
	}

	public IModel getAggregateModel() {
		return findSiteModel();
	}

	private IModel findSiteModel() {
		InputContext scontext = findContext(SiteInputContext.CONTEXT_ID);
		if (scontext!=null)
			return scontext.getModel();
		else
			return null;
	}
}