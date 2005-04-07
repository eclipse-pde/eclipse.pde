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
import java.util.Hashtable;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.*;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class ModifyCommand extends MacroCommand {
	public static final String TYPE = "modify";

	private String text;

	public ModifyCommand(WidgetIdentifier wid) {
		super(wid);
	}

	public String getType() {
		return TYPE;
	}

	public boolean mergeEvent(Event e) {
		return doProcessEvent(e);
	}

	public void processEvent(Event e) {
		doProcessEvent(e);
	}

	protected void load(Node node, Hashtable lineTable) {
		super.load(node, lineTable);

		NodeList children = node.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.TEXT_NODE) {
				text = MacroUtil.getNormalizedText(child.getNodeValue());
				break;
			}
		}
	}

	private boolean doProcessEvent(Event e) {
		String text = extractText(e.widget);
		if (text != null) {
			this.text = text;
			return true;
		}
		return false;
	}

	private String extractText(Widget widget) {
		if (widget instanceof Text)
			return ((Text) widget).getText();
		if (widget instanceof Combo)
			return ((Combo) widget).getText();
		if (widget instanceof CCombo)
			return ((CCombo) widget).getText();
		if (widget instanceof StyledText)
			return MacroUtil.getWritableText(((StyledText) widget).getText());
		return null;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\" contextId=\"");
		writer.print(getWidgetId().getContextId());
		writer.print("\" widgetId=\"");
		writer.print(getWidgetId().getWidgetId());
		writer.println("\">");
		if (text != null) {
			writer.print(indent);
			writer.print(text);
			writer.println();
		}
		writer.print(indent);
		writer.println("</command>");
	}

	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		if (parent.isDisposed()) return false;
		CommandTarget target = MacroUtil.locateCommandTarget(parent,
				getWidgetId(), getStartLine());
		if (target != null) {
			target.setFocus();
			Widget widget = target.getWidget();
			if (widget instanceof Text)
				((Text) widget).setText(text);
			else if (widget instanceof Combo)
				((Combo) widget).setText(text);
			else if (widget instanceof CCombo)
				((CCombo) widget).setText(text);
			else if (widget instanceof StyledText)
				((StyledText)widget).setText(text);
		}
		return true;
	}
}