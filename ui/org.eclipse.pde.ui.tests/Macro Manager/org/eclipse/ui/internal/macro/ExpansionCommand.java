/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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