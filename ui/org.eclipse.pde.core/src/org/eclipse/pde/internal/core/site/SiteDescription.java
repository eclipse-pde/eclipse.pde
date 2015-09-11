/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SiteDescription extends SiteObject implements ISiteDescription {
	private static final long serialVersionUID = 1L;
	private String name;
	private String url;
	private String text;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getURL()
	 */
	@Override
	public String getURL() {
		return url;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getText()
	 */
	@Override
	public String getText() {
		return text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_URL, oldValue, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setURL(java.lang.String)
	 */
	@Override
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.text;
		this.text = text;
		firePropertyChanged(P_TEXT, oldValue, text);
	}

	@Override
	protected void reset() {
		name = null;
		url = null;
		text = null;
	}

	@Override
	protected void parse(Node node) {
		name = getNodeAttribute(node, P_NAME);
		url = getNodeAttribute(node, P_URL);
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				Node firstChild = node.getFirstChild();
				if (firstChild != null)
					text = getNormalizedText(firstChild.getNodeValue());
				break;
			}
		}
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_TEXT)) {
			setText(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if ((name == null || name.length() <= 0) && (url == null || url.length() <= 0) && (text == null || text.trim().length() <= 0))
			return;
		writer.print(indent);
		writer.print("<description"); //$NON-NLS-1$
		if (name != null && name.length() > 0) {
			writer.print(" name=\"" + SiteObject.getWritableString(name) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (url != null && url.length() > 0)
			writer.print(" url=\"" + SiteObject.getWritableString(url) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(">"); //$NON-NLS-1$
		if (text != null) {
			writer.println(indent + Site.INDENT + SiteObject.getWritableString(getNormalizedText(text)));
		}
		writer.println(indent + "</description>"); //$NON-NLS-1$
	}

	@Override
	public boolean isValid() {
		return true;
	}

}
