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


import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

public class StructuredSelectionCommand extends AbstractStructuredCommand {
	public static final String TYPE="item-select";
	private ArrayList items;
	
	public StructuredSelectionCommand(WidgetIdentifier wid) {
		super(wid);
		items = new ArrayList();
	}
	
	public String getType() {
		return TYPE;
	}

	protected Widget[] getItemsForEvent(Event event) {
		if (event.widget instanceof Tree) {
			return ((Tree)event.widget).getSelection();
		}
		else if (event.widget instanceof Table) {
			return ((Table)event.widget).getSelection();
		}
		else if (event.widget instanceof TableTree) {
			return ((TableTree)event.widget).getSelection();
		}
		return super.getItemsForEvent(event);
	}

	protected void playTreeCommand(Tree tree, TreeItem[] matches) {
		tree.setSelection(matches);
		fireEvent(tree);
	}
	
	private void fireEvent(Widget widget) {
		Event e = new Event();
		e.widget = widget;
		e.type = SWT.Selection;
		widget.notifyListeners(e.type, e);
	}

	protected void playTableCommand(Table table, TableItem[] matches) {
		table.setSelection(matches);
		fireEvent(table);
	}
	
	protected void playTableTreeCommand(TableTree tableTree, TableTreeItem [] matches) {
		tableTree.setSelection(matches);
		fireEvent(tableTree);
	}
}