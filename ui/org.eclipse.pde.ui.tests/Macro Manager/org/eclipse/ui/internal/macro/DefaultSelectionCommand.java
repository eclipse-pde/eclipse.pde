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

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class DefaultSelectionCommand extends MacroCommand {
	public static final String TYPE = "default-select";

	/**
	 * @param wid
	 */
	public DefaultSelectionCommand(WidgetIdentifier wid) {
		super(wid);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.SelectionCommand#getKind()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.MacroCommand#processEvent(org.eclipse.swt.widgets.Event)
	 */
	public void processEvent(Event e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\" contextId=\"");
		writer.print(getWidgetId().getContextId());
		writer.print("\" widgetId=\"");
		writer.print(getWidgetId().getWidgetId());
		writer.print("\"");
		writer.println("/>");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.IPlayable#playback(org.eclipse.swt.widgets.Composite)
	 */
	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		CommandTarget target = MacroUtil.locateCommandTarget(parent, getWidgetId());
		if (target==null) return false;
		target.setFocus();
		Widget widget = target.getWidget();
		Event e = new Event();
		e.type = SWT.DefaultSelection;
		e.widget = widget;
		e.display = display;
		widget.notifyListeners(SWT.DefaultSelection, e);
		return true;
	}
}