/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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