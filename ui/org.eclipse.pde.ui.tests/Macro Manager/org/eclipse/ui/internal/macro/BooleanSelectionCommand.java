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
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.w3c.dom.*;

public class BooleanSelectionCommand extends MacroCommand {
	public static final String TYPE="select";
	private Boolean selection;
	private ArrayList path;
	
	public BooleanSelectionCommand(WidgetIdentifier wid) {
		super(wid);
	}

	public String getType() {
		return TYPE;
	}

	public void processEvent(Event e) {
		selection = getSelection(e.widget);
		if (e.widget instanceof MenuItem) {
			//System.out.println("Item="+e.widget+" data = "+e.widget.getData());
			path = getPath((MenuItem)e.widget);
		}
	}
	
	private Boolean getSelection(Widget widget) {
		if ((widget.getStyle() & (SWT.CHECK | SWT.RADIO)) == 0) 
			return null;
		if (widget instanceof Button)
			return new Boolean(((Button)widget).getSelection());
		if (widget instanceof ToolItem)
			return new Boolean(((ToolItem)widget).getSelection());
		if (widget instanceof MenuItem)
			return new Boolean(((MenuItem)widget).getSelection());
		return null;
	}
	
	private ArrayList getPath(MenuItem item) {
		ArrayList segments = new ArrayList();
		Object data = item.getData();
		
		if (data instanceof ContributionItem) {
			ContributionItem aitem = (ContributionItem)data;
			MenuManager manager = (MenuManager)aitem.getParent();
			while (manager!=null) {
				String id = manager.getId();
				if (id==null) 
					break;
				segments.add(0, id);				
				manager = (MenuManager)manager.getParent();
			}
		}
		return segments.size()>0?segments:null;
	}
	
	protected void load(Node node) {
		super.load(node);
		String sel = MacroUtil.getAttribute(node, "selection");
		if (sel!=null) {
			selection = sel.equals("true")?Boolean.TRUE:Boolean.FALSE;
		}
		NodeList children = node.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.ELEMENT_NODE &&
					child.getNodeName().equals("parent")) {
				if (path==null)
					path = new ArrayList();
				path.add(MacroUtil.getAttribute(child, "widgetId"));
			}
		}
	}
	
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\" contextId=\"");
		writer.print(getWidgetId().getContextId());
		writer.print("\" widgetId=\"");
		writer.print(getWidgetId().getWidgetId());
		writer.print("\"");
		if (selection!=null) {
			writer.print(" selection=\"");
			writer.print(selection.equals(Boolean.TRUE)?"true":"false");
			writer.print("\"");
		}
		if (path!=null) {
			writer.println(">");
			String pindent = indent + "   ";
			for (int i=0; i<path.size(); i++) {
				writer.print(pindent);
				writer.print("<parent widgetId=\"");
				writer.print((String)path.get(i));
				writer.println("\"/>");
			}
			writer.print(indent);
			writer.println("</command>");
		}
		else 
			writer.println("/>");
	}

	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		CommandTarget target = MacroUtil.locateCommandTarget(parent, getWidgetId(), path);
		if (target==null) return false;
		target.setFocus();
		Widget widget = target.getWidget();

		if ((widget.getStyle() & (SWT.CHECK | SWT.RADIO)) == 0) {
			doClick(widget);
		}
		else if (selection!=null)
			doSelect(widget);
		return true;
	}
	private void doClick(Widget widget) throws CoreException {
		Event e = new Event();
		e.type = SWT.Selection;
		e.widget = widget;
		widget.notifyListeners(e.type, e);
	}

	private Event createMouseEvent(Widget widget, int type) {
		Event e = new Event();
		e.type = type;
		e.button = 1;
		e.widget = widget;
		return e;
	}
	
	private void doSelect(Widget widget) throws CoreException {
		if (widget instanceof Button)
			((Button)widget).setSelection(selection.booleanValue());
		else if (widget instanceof ToolItem)
			((ToolItem)widget).setSelection(selection.booleanValue());
		else if (widget instanceof MenuItem)
			((MenuItem)widget).setSelection(selection.booleanValue());		
	}
}