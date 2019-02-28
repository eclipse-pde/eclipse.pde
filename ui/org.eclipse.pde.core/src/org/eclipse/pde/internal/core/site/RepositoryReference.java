/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void setURL(String url) throws CoreException {
		String old = fURL;
		fURL = url;
		ensureModelEditable();
		firePropertyChanged(P_LOCATION, old, fURL);
	}

	@Override
	public String getURL() {
		return fURL;
	}

	@Override
	public boolean getEnabled() {
		return fEnabled;
	}

	@Override
	public void setEnabled(boolean enabled) throws CoreException {
		boolean old = fEnabled;
		fEnabled = enabled;
		ensureModelEditable();
		firePropertyChanged(P_ENABLED, old, fEnabled);
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute("location"); //$NON-NLS-1$
			fEnabled = Boolean.parseBoolean(element.getAttribute(P_ENABLED));
		}
	}

	@Override
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

	@Override
	public boolean isValid() {
		return isURLDefined();
	}

}
