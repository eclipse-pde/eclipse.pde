/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FocusCommand extends MacroCommand {
	public static final String TYPE = "focus";
	
	public FocusCommand(WidgetIdentifier wid) {
		super(wid);
	}
	
	public boolean mergeEvent(Event e) {
		// we can directly merge repeated focus requests
		// on the same widget
		return true;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.MacroCommand#getType()
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
		if (parent.isDisposed()) return false;
		CommandTarget target = MacroUtil.locateCommandTarget(parent, getWidgetId());
		if (target!=null)
			target.setFocus();
		return true;
	}
}