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

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Event;
import org.w3c.dom.Node;

public abstract class MacroCommand implements IWritable, IPlayable {
	private WidgetIdentifier widgetId;

	public MacroCommand (WidgetIdentifier widgetId) {
		this.widgetId = widgetId;
	}
	
	public abstract String getType();
	public abstract void processEvent(Event e);
	
	protected void load(Node node) {
		String cid = MacroUtil.getAttribute(node, "contextId");		
		String wid = MacroUtil.getAttribute(node, "widgetId");
		if (wid!=null && cid!=null)
			widgetId = new WidgetIdentifier(new Path(cid), new Path(wid));
	}
	
	public boolean mergeEvent(Event e) {
		return false;
	}
	public WidgetIdentifier getWidgetId() {
		return widgetId;
	}
}