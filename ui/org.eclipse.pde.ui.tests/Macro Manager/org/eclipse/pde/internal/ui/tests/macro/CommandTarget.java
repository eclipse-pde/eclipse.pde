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
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

public class CommandTarget {
	private Widget widget;
	private Object context;
	
	public CommandTarget(Widget widget, Object context) {
		this.widget = widget;
		this.context = context;
	}

	public void ensureVisible() {
	}
	
	public Widget getWidget() {
		return widget;
	}
	public Object getContext() {
		return context;
	}
	public void setFocus() {
		ensureVisible();
		Display display = widget.getDisplay();
		if (widget instanceof Control) {
			Control c = (Control)widget;
			if (!c.equals(display.getFocusControl()))
				c.setFocus();
		}
	}
}