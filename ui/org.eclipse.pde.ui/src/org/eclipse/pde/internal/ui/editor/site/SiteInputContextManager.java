/*
 * Created on Mar 1, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.context.*;

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

	public IBaseModel getAggregateModel() {
		return findSiteModel();
	}

	private IBaseModel findSiteModel() {
		InputContext scontext = findContext(SiteInputContext.CONTEXT_ID);
		if (scontext!=null)
			return scontext.getModel();
		else
			return null;
	}
}