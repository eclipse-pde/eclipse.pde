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

import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

public class CheckCommand extends ToggleStructuredCommand {
	public static final String TYPE = "item-check";
	/**
	 * @param wid
	 */
	public CheckCommand(WidgetIdentifier wid) {
		super(wid);
	}
	
	public String getType() {
		return TYPE;
	}
	
	public void processEvent(Event event) {
		super.processEvent(event);
		Widget item = event.item;
		if (item instanceof TreeItem)
			value = !((TreeItem)item).getChecked();
		else if (item instanceof TableItem)
			value = !((TableItem)item).getChecked();
		else if (item instanceof TableTreeItem)
			value = !((TableTreeItem)item).getChecked();
	}

	protected void playTreeCommand(Tree tree, TreeItem[] matches) {
		for (int i=0; i<matches.length; i++) {
			matches[i].setChecked(getValue());
		}
	}

	protected void playTableCommand(Table table, TableItem[] matches) {
		for (int i=0; i<matches.length; i++) {
			matches[i].setChecked(getValue());
		}		
	}
	
	protected void playTableTreeCommand(TableTree tableTree, TableTreeItem [] matches) {
		for (int i=0; i<matches.length; i++) {
			matches[i].setChecked(getValue());
		}
	}
}