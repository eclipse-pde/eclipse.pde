/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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