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

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.w3c.dom.*;

public abstract class AbstractStructuredCommand extends MacroCommand {
	protected ArrayList items;
	
	public AbstractStructuredCommand(WidgetIdentifier wid) {
		super(wid);
		items = new ArrayList();
	}

	public boolean mergeEvent(Event e) {
		items.clear();
		processEvent(e);
		return true;
	}
	
	protected Widget [] getItemsForEvent(Event e) {
		Widget item = null;
		if (e.item!=null)
			item = (Widget)e.item;
		else if (e.widget instanceof Item)
			item = e.widget;
		if (item!=null)
			return new Widget[] {item};
		return null;
	}
	
	public void processEvent(Event event) {
		Widget [] eventItems=getItemsForEvent(event);

		if (eventItems!=null) {
			for (int i=0; i<eventItems.length; i++) {
				String id = getItemId(eventItems[i]);
				if (id!=null)
					items.add(id);
			}
		}
	}
	
	protected String getItemId(Widget item) {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		String id = recorder.resolveWidget(item);
		if (id!=null)
			return id;
		Object data = item.getData();
		if (data!=null)
			return data.getClass().getName();
		return null;
	}

	protected void load(Node node, Hashtable lineTable) {
		super.load(node, lineTable);
		NodeList children = node.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.ELEMENT_NODE &&
					child.getNodeName().equals("item")) {
				String path = MacroUtil.getAttribute(child, "path");
				if (path!=null)
					items.add(path);
			}
		}
	}
	
	protected void writeAdditionalAttributes(PrintWriter writer) {
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
		writeAdditionalAttributes(writer);
		writer.println(">");
		String cindent = indent + "   ";
		for (int i=0; i<items.size(); i++) {
			writer.print(cindent);
			writer.print("<item path=\"");
			writer.print((String)items.get(i));
			writer.println("\"/>");
		}
		writer.println(indent+"</command>");
	}
	
	protected abstract void playTreeCommand(Tree tree, TreeItem[] matches);
	protected abstract void playTableCommand(Table table, TableItem[] matches);
	protected abstract void playTableTreeCommand(TableTree tableTree, TableTreeItem[] matches);	

	public final boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		CommandTarget target = MacroUtil.locateCommandTarget(parent, getWidgetId(), getStartLine());

		if (target==null) return false;
		target.setFocus();
		MacroUtil.processDisplayEvents(display);

		Widget widget = target.getWidget();
		
		if (widget==null || widget.isDisposed())
			return false;
		
		if (widget instanceof Tree) {
			TreeItem[] matches = findMatches((Tree)widget);
			playTreeCommand((Tree)widget, matches);
		}
		else if (widget instanceof Table) {
			TableItem [] matches = findMatches((Table)widget);
			playTableCommand((Table)widget, matches);
		}
		else if (widget instanceof TableTree) {
			TableTreeItem [] matches = findMatches((TableTree)widget);
			playTableTreeCommand((TableTree)widget, matches);
		}
		return true;
	}

	private TreeItem[] findMatches(Tree tree) {
		TreeItem [] children = tree.getItems();
		ArrayList matches = new ArrayList();
		for (int i=0; i<items.size(); i++) {
			String itemId = (String)items.get(i);
			TreeItem item = findTreeItem(children, itemId);
			if (item!=null)
				matches.add(item);
		}
		return (TreeItem[])matches.toArray(new TreeItem[matches.size()]);
	}
	private TableItem[] findMatches(Table table) {
		TableItem [] elements = table.getItems();
		ArrayList matches = new ArrayList();

		for (int i=0; i<items.size(); i++) {
			String itemId = (String)items.get(i);
			TableItem item = findTableItem(elements, itemId);
			if (item!=null)
				matches.add(item);
		}
		return (TableItem[])matches.toArray(new TableItem[matches.size()]);
	}
	
	private TableTreeItem [] findMatches(TableTree tableTree) {
		TableTreeItem [] children = tableTree.getItems();
		ArrayList matches = new ArrayList();

		for (int i=0; i<items.size(); i++) {
			String itemId = (String)items.get(i);
			TableTreeItem item = findTableTreeItem(children, itemId);
			if (item!=null)
				matches.add(item);
		}
		return (TableTreeItem[])matches.toArray(new TableTreeItem[matches.size()]);
	}

	private TreeItem findTreeItem(TreeItem [] children, String itemId) {
		for (int i=0; i<children.length; i++) {
			TreeItem item = children[i];
			String id = getItemId(item);
			//Test the item itself
			if (id!=null && id.equals(itemId))
				return item;
			int ccount = item.getItemCount();
			if (ccount>0) {
				//Test the item's children
				TreeItem citem = findTreeItem(item.getItems(), itemId);
				if (citem!=null)
					return citem;
			}
		}
		return null;
	}

	private TableItem findTableItem(TableItem [] children, String itemId) {
		for (int i=0; i<children.length; i++) {
			TableItem item = children[i];
			String id = getItemId(item);

			if (id!=null && id.equals(itemId))
				return item;
		}
		return null;
	}
	
	private TableTreeItem findTableTreeItem(TableTreeItem [] children, String itemId) {
		for (int i=0; i<children.length; i++) {
			TableTreeItem item = children[i];
			String id = getItemId(item);
			//Test the item itself
			if (id!=null && id.equals(itemId))
				return item;
			int ccount = item.getItemCount();
			if (ccount>0) {
				//Test the item's children
				TableTreeItem citem = findTableTreeItem(item.getItems(), itemId);
				if (citem!=null)
					return citem;
			}
		}
		return null;
	}
}