package org.eclipse.ui.internal.macro;

import org.eclipse.core.runtime.IPath;

/*
 * Created on Dec 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WidgetIdentifier {
	public IPath contextPath;
	public IPath widgetPath;
	
	public WidgetIdentifier(IPath contextPath, IPath widgetPath) {
		this.contextPath = contextPath;
		this.widgetPath = widgetPath;
	}
	
	public String getContextId() {
		return contextPath.toString();
	}
	public String getWidgetId() {
		return widgetPath.toString();
	}
	public IPath getFullyQualifiedPath() {
		return contextPath.append(widgetPath);
	}
	public String getFullyQualifiedId() {
		return getFullyQualifiedPath().toString();
	}
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (obj==this) return true;
		if (obj instanceof WidgetIdentifier) {
			WidgetIdentifier wid = (WidgetIdentifier)obj;
			return wid.contextPath.equals(contextPath) && wid.widgetPath.equals(widgetPath);
		}
		return false;
	}
}