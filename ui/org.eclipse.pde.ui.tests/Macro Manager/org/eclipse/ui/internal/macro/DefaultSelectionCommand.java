/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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