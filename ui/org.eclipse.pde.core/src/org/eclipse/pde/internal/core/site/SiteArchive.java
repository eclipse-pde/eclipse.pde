/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteArchive extends SiteObject implements ISiteArchive {
	private String url;
	private String path;
	
	public boolean isValid() {
		return url!=null && path!=null;
	}

	public String getURL() {
		return url;
	}
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.path;
		this.path = path;
		firePropertyChanged(P_PATH, oldValue, path);
	}
	public void reset() {
		super.reset();
		url = null;
		path = null;
	}
	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		path = getNodeAttribute(node, "path"); //$NON-NLS-1$
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
		bindSourceLocation(node, lineTable);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<archive"); //$NON-NLS-1$
		if (path != null)
			writer.print(" path=\"" + path + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (url != null)
			writer.print(" url=\"" + url + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_PATH)) {
			setPath(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

}