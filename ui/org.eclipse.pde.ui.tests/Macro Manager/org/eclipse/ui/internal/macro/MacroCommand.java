/*
 * Created on Nov 25, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Event;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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