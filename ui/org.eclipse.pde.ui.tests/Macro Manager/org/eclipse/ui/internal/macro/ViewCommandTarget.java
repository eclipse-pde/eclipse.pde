/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.*;
import org.eclipse.ui.IViewPart;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ViewCommandTarget extends CommandTarget {
	public ViewCommandTarget(Widget widget, IViewPart view) {
		super(widget, view);
	}
	
	public IViewPart getView() {
		return (IViewPart)getContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.CommandTarget#ensureVisible()
	 */
	public void ensureVisible() {
		IViewPart view = getView();
		IWorkbenchPage page = view.getViewSite().getPage();
		page.activate(view);
	}
}