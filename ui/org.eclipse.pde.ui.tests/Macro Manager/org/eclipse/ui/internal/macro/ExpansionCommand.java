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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

public class ExpansionCommand extends ToggleStructuredCommand {
	public static final String TYPE = "item-expand";
	/**
	 * @param wid
	 */
	public ExpansionCommand(WidgetIdentifier wid) {
		super(wid);
	}
	
	public void processEvent(Event event) {
		super.processEvent(event);
		Widget item = event.item;

		if (item instanceof TreeItem)
			value = !((TreeItem)item).getExpanded();
		else if (item instanceof TableTreeItem)
			value = !((TableTreeItem)item).getExpanded();
	}

	protected void playTreeCommand(Tree tree, TreeItem[] matches) {
		for (int i=0; i<matches.length; i++) {
			matches[i].setExpanded(getValue());
			fireEvent(tree, matches[i]);
		}
	}
	
	private void fireEvent(Widget widget, Widget item) {
		Event event = new Event();
		event.type = getValue()?SWT.Expand:SWT.Collapse;
		event.widget = widget;
		event.item= item;
		widget.notifyListeners(event.type, event);
	}

	protected void playTableCommand(Table table, TableItem[] matches) {
	}
	
	protected void playTableTreeCommand(TableTree tableTree, TableTreeItem [] matches) {
		for (int i=0; i<matches.length; i++) {
			matches[i].setExpanded(getValue());
			fireEvent(tableTree, matches[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.macro.SelectionCommand#getKind()
	 */
	public String getType() {
		return TYPE;
	}
}