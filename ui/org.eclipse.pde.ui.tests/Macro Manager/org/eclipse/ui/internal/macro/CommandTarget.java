/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Widget;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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