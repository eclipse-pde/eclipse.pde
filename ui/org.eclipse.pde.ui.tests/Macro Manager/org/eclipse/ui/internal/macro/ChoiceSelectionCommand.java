/*
 * Created on Nov 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.macro.MacroPlugin;
import org.w3c.dom.*;

/**
 * @author dejan
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ChoiceSelectionCommand extends MacroCommand {
	public static final String TYPE = "choice-select";

	private String choiceId;

	public ChoiceSelectionCommand(WidgetIdentifier wid) {
		super(wid);
	}

	public String getType() {
		return TYPE;
	}

	public void processEvent(Event e) {
		choiceId = computeChoiceId(e.widget, e.item);
	}

	private String computeChoiceId(Widget widget, Widget item) {
		int index = -1;

		if (widget instanceof Combo) {
			Combo combo = (Combo) widget;
			index = combo.getSelectionIndex();
		} else if (widget instanceof CCombo) {
			CCombo combo = (CCombo) widget;
			index = combo.getSelectionIndex();
		} else {
			String id = MacroPlugin.getDefault().getMacroManager()
					.resolveWidget(item);
			if (id != null)
				return id;
			if (widget instanceof TabFolder) {
				TabFolder tabFolder = (TabFolder) widget;
				TabItem tabItem = (TabItem) item;
				index = tabFolder.indexOf(tabItem);
			} else if (widget instanceof CTabFolder) {
				CTabFolder tabFolder = (CTabFolder) widget;
				CTabItem tabItem = (CTabItem) item;
				index = tabFolder.indexOf(tabItem);
			}
		}
		if (index != -1)
			return getPositionId(index);
		else
			return null;
	}

	private String getPositionId(int index) {
		return "item#" + index;
	}

	protected void load(Node node) {
		super.load(node);
		choiceId = MacroUtil.getAttribute(node, "choiceId");
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
		if (choiceId != null) {
			writer.print(" choiceId=\"");
			writer.print(choiceId);
			writer.print("\"");
		}
		writer.println("/>");
	}

	public boolean playback(Display display, Composite parent, IProgressMonitor monitor)
			throws CoreException {
		CommandTarget target = MacroUtil.locateCommandTarget(parent,
				getWidgetId());
		if (target == null)
			return false;
		target.setFocus();
		Widget widget = target.getWidget();
		if (widget instanceof TabFolder)
			doSelect((TabFolder) widget);
		else if (widget instanceof CTabFolder)
			doSelect((CTabFolder) widget);
		else if (widget instanceof Combo)
			doSelect((Combo) widget);
		else if (widget instanceof CCombo)
			doSelect((CCombo) widget);
		return true;
	}

	private void doSelect(TabFolder tabFolder) {
		TabItem[] items = tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			TabItem item = items[i];
			String id = computeChoiceId(tabFolder, item);
			if (id != null && id.equals(choiceId)) {
				tabFolder.setSelection(i);
				break;
			}
		}
	}

	private void doSelect(CTabFolder tabFolder) {
		CTabItem[] items = tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			CTabItem item = items[i];
			String id = computeChoiceId(tabFolder, item);
			if (id != null && id.equals(choiceId)) {
				tabFolder.setSelection(i);
				break;
			}
		}
	}

	private void doSelect(Combo combo) {
		int index = getIndexFromChoice();
		if (index != -1)
			combo.select(index);
	}

	private void doSelect(CCombo combo) {
		int index = getIndexFromChoice();
		if (index != -1)
			combo.select(index);
	}

	private int getIndexFromChoice() {
		int loc = choiceId.indexOf('#');
		if (loc == -1)
			return -1;
		try {
			return new Integer(choiceId.substring(loc + 1)).intValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}