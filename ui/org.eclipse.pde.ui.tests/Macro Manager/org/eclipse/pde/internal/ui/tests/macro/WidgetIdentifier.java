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

import org.eclipse.core.runtime.IPath;

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