/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

public class SiteDescription extends SiteObject implements ISiteDescription {
	private static final long serialVersionUID = 1L;
	private String url;
	private String text;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getURL()
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getText()
	 */
	public String getText() {
		return text;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setURL(java.net.URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setText(java.lang.String)
	 */
	public void setText(String text) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.text;
		this.text = text;
		firePropertyChanged(P_TEXT, oldValue, text);
	}

	protected void reset() {
		url = null;
		text = null;
	}

	protected void parse(Node node, Hashtable lineTable) {
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
		bindSourceLocation(node, lineTable);
		NodeList children = node.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.TEXT_NODE) {
				Node firstChild = node.getFirstChild();
				if (firstChild!=null)
					text = getNormalizedText(firstChild.getNodeValue());
				break;
			}
		}
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_TEXT)) {
			setText(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}
	
	public void write(String indent, PrintWriter writer) {
		if ((url == null || url.length() <= 0)
				&& (text == null || text.trim().length() <= 0))
			return;
		writer.print(indent);
		writer.print("<description"); //$NON-NLS-1$
		if (url != null && url.length() > 0)
			writer.print(" url=\""+url+"\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(">"); //$NON-NLS-1$
		if (text!=null) {
			writer.println(indent+Site.INDENT+ getNormalizedText(text));
		}
		writer.println(indent+"</description>"); //$NON-NLS-1$
	}
	public boolean isValid() {
		return true;
	}

}
