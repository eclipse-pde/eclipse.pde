/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public boolean isValid() {
		return url != null && path != null;
	}

	@Override
	public String getURL() {
		return url;
	}

	@Override
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public void setPath(String path) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.path;
		this.path = path;
		firePropertyChanged(P_PATH, oldValue, path);
	}

	@Override
	public void reset() {
		super.reset();
		url = null;
		path = null;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		path = getNodeAttribute(node, "path"); //$NON-NLS-1$
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<archive"); //$NON-NLS-1$
		if (path != null) {
			writer.print(" path=\"" + SiteObject.getWritableString(path) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (url != null) {
			writer.print(" url=\"" + SiteObject.getWritableString(url) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_PATH)) {
			setPath(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

}
