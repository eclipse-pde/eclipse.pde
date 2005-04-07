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

import org.eclipse.swt.widgets.Event;
import org.w3c.dom.Node;

public abstract class ToggleStructuredCommand extends AbstractStructuredCommand {
	protected boolean value;

	/**
	 * @param wid
	 */
	public ToggleStructuredCommand(WidgetIdentifier wid) {
		super(wid);
	}
	
	public boolean mergeEvent(Event e) {
		return false;
	}
	
	protected void load(Node node, Hashtable lineTable) {
		super.load(node, lineTable);
		String att = MacroUtil.getAttribute(node, "value");
		this.value = att!=null && att.equals("true");
	}
	
	protected void writeAdditionalAttributes(PrintWriter writer) {
		writer.print(" value=\"");
		writer.print(value?"true":"false");
		writer.print("\"");
	}
	
	public boolean getValue() {
		return value;
	}
}
