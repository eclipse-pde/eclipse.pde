/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.IRepositoryReference;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class RepositoryReference extends SiteObject implements IRepositoryReference {

	private static final long serialVersionUID = 1L;

	public static final String P_LOCATION = "location"; //$NON-NLS-1$
	public static final String P_ENABLED = "enabled"; //$NON-NLS-1$

	private String fURL;
	private boolean fEnabled = true; // enabled unless specified otherwise

	public RepositoryReference() {
		super();
	}

	public void setURL(String url) throws CoreException {
		String old = fURL;
		fURL = url;
		ensureModelEditable();
		firePropertyChanged(P_LOCATION, old, fURL);
	}

	public String getURL() {
		return fURL;
	}

	public boolean getEnabled() {
		return fEnabled;
	}

	public void setEnabled(boolean enabled) throws CoreException {
		boolean old = fEnabled;
		fEnabled = enabled;
		ensureModelEditable();
		firePropertyChanged(P_ENABLED, old, fEnabled);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute("location"); //$NON-NLS-1$
			fEnabled = Boolean.valueOf(element.getAttribute(P_ENABLED)).booleanValue();
		}
	}

	public void write(String indent, PrintWriter writer) {
		if (isURLDefined()) {
			writer.print(indent + "<repository-reference location=\"" + fURL + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(" enabled=\"" + fEnabled + "\""); //$NON-NLS-1$//$NON-NLS-2$
			writer.println(" />"); //$NON-NLS-1$
		}
	}

	private boolean isURLDefined() {
		return fURL != null && fURL.length() > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteObject#isValid()
	 */
	public boolean isValid() {
		return isURLDefined();
	}

}
