/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.w3c.dom.Node;

public class SiteArchive extends SiteObject implements ISiteArchive {
	private static final long serialVersionUID = 1L;
	private String url;
	private String path;

	public boolean isValid() {
		return url != null && path != null;
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

	protected void parse(Node node) {
		super.parse(node);
		path = getNodeAttribute(node, "path"); //$NON-NLS-1$
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<archive"); //$NON-NLS-1$
		if (path != null)
			writer.print(" path=\"" + SiteObject.getWritableString(path) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (url != null)
			writer.print(" url=\"" + SiteObject.getWritableString(url) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_PATH)) {
			setPath(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

}
