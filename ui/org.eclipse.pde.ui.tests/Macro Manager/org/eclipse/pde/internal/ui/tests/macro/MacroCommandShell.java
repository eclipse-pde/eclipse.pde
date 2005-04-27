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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IWorkbenchCommandSupport;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.KeyStroke;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MacroCommandShell extends MacroInstruction {
	private ArrayList commands;

	private int expectedReturnCode;

	private transient Event lastEvent;

	private transient Display display;

	private transient Shell shell;

	private transient IIndexHandler indexHandler;

	private transient Window window;

	private static class NestedShell implements Listener, Runnable {
		private MacroCommandShell cshell;

		private Display display;

		private Shell nshell;

		private boolean released;

		private CoreException exception;

		private IProgressMonitor monitor;

		public NestedShell(Display display, MacroCommandShell cshell,
				IProgressMonitor monitor) {
			this.display = display;
			this.cshell = cshell;
			this.monitor = monitor;
		}

		public void handleEvent(Event e) {
			if (e.widget instanceof Shell) {
				// shell activated
				Shell shell = (Shell) e.widget;
				IPath path = MacroUtil.getShellId(shell);
				String sid = path.toString();
				if (sid.equals(cshell.getId())) {
					shell.getDisplay().removeFilter(SWT.Activate, this);
					released = true;
					this.nshell = shell;
					shell.getDisplay().asyncExec(this);
				}
			}
		}

		public boolean getResult() {
			return cshell.matchesReturnCode();
		}

		public boolean isReleased() {
			return released;
		}

		public void run() {
			try {
				cshell.playback(display, nshell, monitor);
			} catch (CoreException e) {
				this.exception = e;
				if (nshell != null && !nshell.isDisposed())
					nshell.close();
			}
		}

		public CoreException getException() {
			return exception;
		}
	}

	public MacroCommandShell() {
		this(null, null);
	}

	public MacroCommandShell(Shell shell, String path) {
		super(path);
		commands = new ArrayList();
		this.shell = shell;
		hookWindow(false);
	}

	private void hookWindow(boolean playback) {
		if (shell != null) {
			if (!playback)
				doHookWindow();
			else
				display.syncExec(new Runnable() {
					public void run() {
						doHookWindow();
					}
				});
		}
	}

	private void doHookWindow() {
		Object data = shell.getData();
		if (data != null && data instanceof Window)
			this.window = (Window) data;
	}

	public void load(Node node, Hashtable lineTable) {
		super.load(node, lineTable);

		String codeId = MacroUtil.getAttribute(node, "return-code");
		if (codeId != null) {
			try {
				expectedReturnCode = new Integer(codeId).intValue();
			} catch (NumberFormatException e) {
			}
		}
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				if (name.equals("command"))
					processCommand(child, lineTable);
				else if (name.equals("shell"))
					processShell(child, lineTable);
				else if (name.equals("index"))
					processIndex(child, lineTable);
			}
		}
	}

	private void processCommand(Node node, Hashtable lineTable) {
		String wid = MacroUtil.getAttribute(node, "widgetId");
		String cid = MacroUtil.getAttribute(node, "contextId");
		String type = MacroUtil.getAttribute(node, "type");
		if (type == null)
			return;
		MacroCommand command = null;
		WidgetIdentifier wi = (wid != null && cid != null) ? new WidgetIdentifier(
				new Path(wid), new Path(cid))
				: null;
		if (type.equals(ModifyCommand.TYPE))
			command = new ModifyCommand(wi);
		else if (type.equals(BooleanSelectionCommand.TYPE))
			command = new BooleanSelectionCommand(wi);
		else if (type.equals(StructuredSelectionCommand.ITEM_SELECT)
				|| type.equals(StructuredSelectionCommand.DEFAULT_SELECT))
			command = new StructuredSelectionCommand(wi, type);
		else if (type.equals(ExpansionCommand.TYPE))
			command = new ExpansionCommand(wi);
		else if (type.equals(CheckCommand.TYPE))
			command = new CheckCommand(wi);
		else if (type.equals(FocusCommand.TYPE))
			command = new FocusCommand(wi);
		else if (type.equals(ChoiceSelectionCommand.TYPE))
			command = new ChoiceSelectionCommand(wi);
		else if (type.equals(WaitCommand.TYPE))
			command = new WaitCommand();
		if (command != null) {
			command.load(node, lineTable);
			commands.add(command);
		}
	}

	private void processShell(Node node, Hashtable lineTable) {
		MacroCommandShell shell = new MacroCommandShell();
		shell.load(node, lineTable);
		commands.add(shell);
	}

	private void processIndex(Node node, Hashtable lineTable) {
		MacroIndex index = new MacroIndex();
		index.load(node, lineTable);
		commands.add(index);
	}

	public void addCommandShell(MacroCommandShell cshell) {
		commands.add(cshell);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<shell id=\"");
		writer.print(getId());
		writer.print("\" return-code=\"");
		writer.print(expectedReturnCode + "");
		writer.println("\">");
		String cindent = indent + "   ";
		for (int i = 0; i < commands.size(); i++) {
			IWritable writable = (IWritable) commands.get(i);
			if (i < commands.size() - 1 || !(writable instanceof WaitCommand))
				writable.write(cindent, writer);
		}
		writer.print(indent);
		writer.println("</shell>");
	}

	public void addEvent(Event event) {
		if (event.widget instanceof Control) {
			if (((Control) event.widget).isVisible() == false)
				return;
		}
		MacroCommand command = createCommand(event);
		if (command != null) {
			command.processEvent(event);
			MacroCommand lastCommand = getLastCommand();
			if (lastCommand != null
					&& lastCommand.getWidgetId().equals(command.getWidgetId())
					&& lastCommand.getType().equals(FocusCommand.TYPE)
					&& isFocusCommand(command.getType())) {
				// focus followed by select or modify - focus implied
				commands.remove(lastCommand);
			}
			commands.add(command);
			lastEvent = event;
		}
	}

	public void addPause() {
		WaitCommand command = new WaitCommand();
		MacroCommand lastCommand = getLastCommand();
		if (lastCommand != null && lastCommand.getType() != WaitCommand.TYPE)
			commands.add(command);
	}

	public void addIndex(String id) {
		commands.add(new MacroIndex(id));
	}

	public void extractExpectedReturnCode() {
		if (window != null)
			expectedReturnCode = window.getReturnCode();
	}

	public boolean matchesReturnCode() {
		if (window != null) {
			return window.getReturnCode() == expectedReturnCode;
		}
		return true;
	}

	private boolean isFocusCommand(String type) {
		return type.equals(BooleanSelectionCommand.TYPE)
				|| type.equals(StructuredSelectionCommand.ITEM_SELECT)
				|| type.equals(StructuredSelectionCommand.DEFAULT_SELECT)
				|| type.equals(ExpansionCommand.TYPE)
				|| type.equals(CheckCommand.TYPE)
				|| type.equals(ModifyCommand.TYPE);
	}

	protected MacroCommand createCommand(Event event) {
		MacroCommand lastCommand = getLastCommand();
		if (lastEvent != null && lastEvent.widget.equals(event.widget)) {
			if (lastEvent.type == event.type
					|| (lastEvent.type == SWT.Selection && event.type == SWT.DefaultSelection)) {
				if (lastCommand != null && lastCommand.mergeEvent(event))
					return null;
			}
		}
		MacroCommand command = null;
		WidgetIdentifier wi = MacroUtil.getWidgetIdentifier(event.widget);
		if (wi == null)
			return null;

		switch (event.type) {
		case SWT.Modify:
			if (!isEditable(event.widget))
				return null;
			command = new ModifyCommand(wi);
			break;
		case SWT.Selection:
		case SWT.DefaultSelection:
			command = createSelectionCommand(wi, event);
			break;
		case SWT.FocusIn:
			command = new FocusCommand(wi);
			break;
		case SWT.Expand:
		case SWT.Collapse:
			command = new ExpansionCommand(wi);
			break;
		/*
		 * case SWT.KeyUp: command = findKeyBinding(wi, event); break;
		 */
		}
		return command;
	}

	private boolean isEditable(Widget widget) {
		if (widget instanceof Control) {
			Control control = (Control) widget;
			if (!control.isEnabled())
				return false;
			if (control instanceof Text)
				return ((Text) control).getEditable();
			if (control instanceof Combo || control instanceof CCombo)
				return ((control.getStyle() & SWT.READ_ONLY) == 0);
			if (control instanceof StyledText)
				return ((StyledText) control).getEditable();
		}
		return true;
	}

	private MacroCommand createSelectionCommand(WidgetIdentifier wid,
			Event event) {
		if (event.widget instanceof MenuItem
				|| event.widget instanceof ToolItem
				|| event.widget instanceof Button) {
			String wId = wid.getWidgetId();
			if (wId.endsWith("org.eclipse.pde.ui.tests.StopAction"))
				return null;
			if (wId.endsWith("org.eclipse.pde.ui.tests.IndexAction"))
				return null;
			return new BooleanSelectionCommand(wid);
		}
		if (event.widget instanceof Tree || event.widget instanceof Table
				|| event.widget instanceof TableTree) {
			if (event.detail == SWT.CHECK)
				return new CheckCommand(wid);
			String type = event.type == SWT.DefaultSelection ? StructuredSelectionCommand.DEFAULT_SELECT
						: StructuredSelectionCommand.ITEM_SELECT;
			return new StructuredSelectionCommand(wid, type);
		}
		if (event.widget instanceof TabFolder
				|| event.widget instanceof CTabFolder)
			return new ChoiceSelectionCommand(wid);
		if (event.widget instanceof Combo || event.widget instanceof CCombo)
			return new ChoiceSelectionCommand(wid);
		return null;
	}

	private MacroCommand findKeyBinding(WidgetIdentifier wid, Event e) {
		System.out.println("mask=" + e.stateMask + ", char=" + e.character);
		java.util.List keyStrokes = MacroUtil.generatePossibleKeyStrokes(e);
		if (keyStrokes.size() == 0)
			return null;
		for (int i = 0; i < keyStrokes.size(); i++) {
			if (!((KeyStroke) keyStrokes.get(i)).isComplete())
				return null;
		}
		System.out.println("keyStrokes=" + keyStrokes);
		IWorkbenchCommandSupport csupport = PlatformUI.getWorkbench()
				.getCommandSupport();
		KeySequence keySequence = KeySequence.getInstance(keyStrokes);
		System.out.println("keySequence=" + keySequence);
		String commandId = csupport.getCommandManager().getPerfectMatch(
				keySequence);
		System.out.println("Command id=" + commandId);
		if (commandId == null)
			return null;
		return new KeyCommand(wid, commandId);
	}

	private MacroCommand getLastCommand() {
		if (commands.size() > 0) {
			Object item = commands.get(commands.size() - 1);
			if (item instanceof MacroCommand)
				return (MacroCommand) item;
		}
		return null;
	}

	public boolean isDisposed() {
		return this.shell != null && this.shell.isDisposed();
	}

	public boolean tracks(Shell shell) {
		if (this.shell != null && this.shell.equals(shell))
			return true;
		return false;
	}

	public boolean playback(final Display display, Composite parent,
			IProgressMonitor monitor) throws CoreException {
		if (parent instanceof Shell) {
			this.shell = (Shell) parent;
			this.display = display;
			hookWindow(true);
		}

		NestedShell nestedShell = null;

		monitor.beginTask("", commands.size());

		for (int i = 0; i < commands.size(); i++) {
			Object c = commands.get(i);
			if (c instanceof MacroIndex) {
				String id = ((MacroIndex) c).getId();
				if (id != null && indexHandler != null) {
					IStatus status = indexHandler.processIndex(shell, id);
					if (status.getSeverity() == IStatus.OK)
						continue;
					throw new CoreException(status);
				}
				// ignore the index
				continue;
			}
			IPlayable playable = (IPlayable) c;
			if (i < commands.size() - 1) {
				// check the next command
				IPlayable next = (IPlayable) commands.get(i + 1);
				if (next instanceof MacroCommandShell) {
					// this command will open a new shell
					// add a listener before it is too late
					MacroCommandShell nestedCommand = (MacroCommandShell) next;
					nestedShell = new NestedShell(display, nestedCommand,
							new SubProgressMonitor(monitor, 1));
					final NestedShell fnestedShell = nestedShell;
					display.syncExec(new Runnable() {
						public void run() {
							display.addFilter(SWT.Activate, fnestedShell);
						}
					});
				}
			}
			if (playable instanceof MacroCommand) {
				boolean last = i == commands.size() - 1;
				playInGUIThread(display, playable, last, monitor);
				monitor.worked(1);
			} else if (nestedShell != null) {
				CoreException e = null;
				if (nestedShell.isReleased() == false) {
					final NestedShell fnestedShell = nestedShell;
					display.syncExec(new Runnable() {
						public void run() {
							display.removeFilter(SWT.Activate, fnestedShell);
						}
					});
				}
				e = nestedShell.getException();
				boolean result = nestedShell.getResult();
				nestedShell = null;
				if (e != null)
					throw e;
				if (!result)
					return false;
			}
		}
		shell = null;
		return true;
	}

	void addExistingIndices(ArrayList list) {
		for (int i = 0; i < commands.size(); i++) {
			Object c = commands.get(i);
			if (c instanceof MacroIndex) {
				list.add(((MacroIndex) c).getId());
			} else if (c instanceof MacroCommandShell) {
				((MacroCommandShell) c).addExistingIndices(list);
			}
		}
	}

	private void playInGUIThread(final Display display,
			final IPlayable playable, boolean last,
			final IProgressMonitor monitor) throws CoreException {
		final CoreException[] ex = new CoreException[1];

		Runnable runnable = new Runnable() {
			public void run() {
				try {
					playable.playback(display, MacroCommandShell.this.shell,
							monitor);
					MacroUtil.processDisplayEvents(display);
				} catch (ClassCastException e) {
					ex[0] = createPlaybackException(playable, e);
				} catch (CoreException e) {
					ex[0] = e;
				} catch (SWTException e) {
					ex[0] = createPlaybackException(playable, e);
				} catch (SWTError error) {
					ex[0] = createPlaybackException(playable, error);
				}
			}
		};
		// if (last)
		// shell.getDisplay().asyncExec(runnable);
		// else
		// display.syncExec(runnable);
		if (playable instanceof WaitCommand) {
			playable.playback(display, this.shell, monitor);
		} else
			display.syncExec(runnable);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}

		// for (;;) {
		// if (display.isDisposed() || !display.readAndDispatch())
		// break;
		// }

		if (ex[0] != null)
			throw ex[0];
	}

	public IIndexHandler getIndexHandler() {
		return indexHandler;
	}

	public void setIndexHandler(IIndexHandler indexHandler) {
		this.indexHandler = indexHandler;
		for (int i = 0; i < commands.size(); i++) {
			Object c = commands.get(i);
			if (c instanceof MacroCommandShell) {
				MacroCommandShell child = (MacroCommandShell) c;
				child.setIndexHandler(indexHandler);
			}
		}
	}
	
	private CoreException createPlaybackException(IPlayable playable, Throwable th) {
		IStatus status = new Status(IStatus.ERROR, "org.eclipse.pde.ui.tests", IStatus.OK,
				"Error while executing a macro command: "+playable.toString(), th);
		return new CoreException(status);		
	}
}