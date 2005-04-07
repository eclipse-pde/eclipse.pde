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
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Node;

public class Macro implements IWritable, IPlayable {
	private static final String SYNTAX_VERSION="0.1";
	private transient Event lastEvent;
	private transient IIndexHandler indexHandler;
	ArrayList shells;
	private Stack shellStack;
	
	public Macro() {
		shells = new ArrayList();
	}
	
	void addShell(Node node, Hashtable lineTable) {
		String sid = MacroUtil.getAttribute(node, "id");
		MacroCommandShell shell = new MacroCommandShell(null, sid);
		shell.load(node, lineTable);
		shells.add(shell);
	}
	
	public void initializeForRecording(Display display) {
		shellStack = new Stack();
		shells.clear();
		Shell currentShell = display.getActiveShell();
		MacroCommandShell commandShell = createCommandShell(currentShell);
		shellStack.push(commandShell);
		shells.add(commandShell);
	}
	
	private MacroCommandShell createCommandShell(Shell shell) {
		WidgetIdentifier wi = MacroUtil.getWidgetIdentifier(shell);
		if (wi==null) return null;
		return new MacroCommandShell(shell, wi.getWidgetId());		
	}
	
	private boolean isCurrent(Shell shell) {
		if (shellStack.isEmpty()) return false;
		MacroCommandShell cshell = (MacroCommandShell)shellStack.peek();
		return cshell.tracks(shell);
	}
	
	public void stopRecording() {
		reset();
	}
	
	public boolean addEvent(Event event) throws Exception {
		if (isIgnorableEvent(event))
			return false;
		try {
			if (event.widget instanceof Shell) {
				switch (event.type) {
					case SWT.Activate:
						activateShell((Shell)event.widget);
						break;
					case SWT.Close:
						boolean stop = closeShell((Shell)event.widget);
						if (stop)
							return true;
						break;
				}
			}
			else if (getTopShell()!=null) {
				getTopShell().addEvent(event);
			}
		}
		catch (Exception e) {
			throw e;
		}
		return false;
	}
	
	private boolean isIgnorableEvent(Event e) {
		Shell shell = e.display.getActiveShell();
		if (shell!=null) {
			Boolean ivalue = (Boolean)shell.getData(MacroManager.IGNORE);
			if (ivalue!=null && ivalue.equals(Boolean.TRUE))
				return true;
		}
		return false;
	}
	
	public void addPause() {
		getTopShell().addPause();
	}
	
	public void addIndex(String id) {
		getTopShell().addIndex(id);
	}
	public MacroCommandShell getTopShell() {
		if (shellStack.isEmpty())
			return null;
		return (MacroCommandShell)shellStack.peek();		
	}
	private void activateShell(Shell shell) {
		Object data = shell.getData();
		if (data instanceof Dialog) {
			if (!isCurrent(shell)) {
				MacroCommandShell commandShell = createCommandShell(shell);
				getTopShell().addCommandShell(commandShell);
				shellStack.push(commandShell);
			}
		}
		else if (data instanceof Window) {
			updateStack();
			if (!isCurrent(shell)) {
				// pop the current
				popStack();		
				MacroCommandShell commandShell = createCommandShell(shell);				
				shellStack.push(commandShell);
				shells.add(commandShell);
			}
		}
	}

	private void popStack() {
		if (shellStack.isEmpty()) return;
		MacroCommandShell top = (MacroCommandShell)shellStack.pop();
		top.extractExpectedReturnCode();
	}

	private boolean closeShell(Shell shell) {
		if (shellStack.isEmpty()) return false;
		MacroCommandShell top = (MacroCommandShell)shellStack.peek();
		if (top.tracks(shell))
			popStack();
		return shellStack.isEmpty();
	}
	
	private void updateStack() {
		while (shellStack.size()>0) {
			MacroCommandShell top = getTopShell();
			if (top.isDisposed())
				popStack();
			else
				break;
		}
	}
	
	public String [] getExistingIndices() {
		ArrayList list = new ArrayList();
		for (int i=0; i<shells.size(); i++) {
			MacroCommandShell shell = (MacroCommandShell)shells.get(i);
			shell.addExistingIndices(list);
		}
		return (String[])list.toArray(new String[list.size()]);
	}
	
	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		reset();
		monitor.beginTask("Executing macro...", shells.size());
		for (int i=0; i<shells.size(); i++) {
			MacroCommandShell shell = (MacroCommandShell)shells.get(i);
			shell.setIndexHandler(getIndexHandler());
			final Shell [] sh = new Shell[1];
			display.syncExec(new Runnable() {
				public void run() {
					sh[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				}
			});
			boolean result = shell.playback(display, sh[0], new SubProgressMonitor(monitor, 1));
			if (!result)
				return false;
		}
		return true;
	}
	private void reset() {
		lastEvent = null;
		shellStack = null;
	}
	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.print("<macro version=\"");
		writer.print(SYNTAX_VERSION);
		writer.println("\">");
		String cindent = "   ";
		for (int i=0; i<shells.size(); i++) {
			MacroCommandShell cshell = (MacroCommandShell)shells.get(i);
			cshell.write(cindent, writer);
		}
		writer.println("</macro>");
	}

	public IIndexHandler getIndexHandler() {
		return indexHandler;
	}
	

	public void setIndexHandler(IIndexHandler indexHandler) {
		this.indexHandler = indexHandler;
	}
	
}