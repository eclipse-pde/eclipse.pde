/*
 * Created on Dec 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.*;
import org.eclipse.ui.commands.IWorkbenchCommandSupport;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class KeyCommand extends MacroCommand {
	public static final String TYPE="key-binding";
	private String commandId;

	/**
	 * @param widgetId
	 */
	public KeyCommand(WidgetIdentifier widgetId, String commandId) {
		super(widgetId);
		this.commandId = commandId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.macro.MacroCommand#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.macro.MacroCommand#processEvent(org.eclipse.swt.widgets.Event)
	 */
	public void processEvent(Event e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.macro.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\" contextId=\"");
		writer.print(getWidgetId().getContextId());
		writer.print("\" widgetId=\"");
		writer.print(getWidgetId().getWidgetId());
		writer.print("\" commandId=\"");
		writer.print(commandId);
		writer.println("\"/>");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.macro.IPlayable#playback(org.eclipse.swt.widgets.Display, org.eclipse.swt.widgets.Composite, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean playback(Display display, Composite parent,
			IProgressMonitor monitor) throws CoreException {
		CommandTarget target = MacroUtil.locateCommandTarget(parent, getWidgetId());
		if (target==null) return false;
		IWorkbenchCommandSupport csupport = PlatformUI.getWorkbench().getCommandSupport();
		ICommand command = csupport.getCommandManager().getCommand(commandId);
		if (command!=null) {
			try {
				command.execute(null);
				return true;
			}
			catch (ExecutionException e) {
				MacroPlugin.logException(e);
			}
			catch (NotHandledException e) {
				MacroPlugin.logException(e);
			}
		}
		return false;
	}

}
