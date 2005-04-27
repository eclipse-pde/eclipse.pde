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

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Event;
import org.w3c.dom.Node;

public abstract class MacroCommand implements IWritable, IPlayable {
	private WidgetIdentifier widgetId;
    private int [] range;

	public MacroCommand (WidgetIdentifier widgetId) {
		this.widgetId = widgetId;
	}
	
	public abstract String getType();
	public abstract void processEvent(Event e);
	
	protected void load(Node node, Hashtable lineTable) {
		String cid = MacroUtil.getAttribute(node, "contextId");		
		String wid = MacroUtil.getAttribute(node, "widgetId");
		if (wid!=null && cid!=null)
			widgetId = new WidgetIdentifier(new Path(cid), new Path(wid));
        bindSourceLocation(node, lineTable);
	}
    
    
    void bindSourceLocation(Node node, Map lineTable) {
        Integer[] lines = (Integer[]) lineTable.get(node);
        if (lines != null) {
            range = new int[2];
            range[0] = lines[0].intValue();
            range[1] = lines[1].intValue();
        }
    }

    public int getStartLine() {
        if (range == null)
            return -1;
        return range[0];
    }
    public int getStopLine() {
        if (range == null)
            return -1;
        return range[1];
    }
	
	public boolean mergeEvent(Event e) {
		return false;
	}
	public WidgetIdentifier getWidgetId() {
		return widgetId;
	}
	public String toString() {
		return "MacroCommand ["+getType()+", line "+getStartLine()+"]";
	}
}