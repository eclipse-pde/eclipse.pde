/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.ILicenseInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LicenseInfo extends ProductObject implements ILicenseInfo {

	public static final String P_URL = "url"; //$NON-NLS-1$
	public static final String P_LICENSE = "license"; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;

	private String fURL;
	private String fLicense;

	public LicenseInfo(IProductModel model) {
		super(model);
	}

	public void setURL(String url) {
		String old = fURL;
		fURL = url;
		if (isEditable())
			firePropertyChanged(P_URL, old, fURL);
	}

	public String getURL() {
		return fURL;
	}

	public String getLicense() {
		return fLicense;
	}

	public void setLicense(String text) {
		String old = fLicense;
		fLicense = text;
		if (isEditable())
			firePropertyChanged(P_LICENSE, old, fLicense);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute(P_URL);
		}
	}

	public void write(String indent, PrintWriter writer) {
		if (fURL != null && fURL.length() > 0)
			writer.println(indent + "<license " + P_URL + "=\"" + getWritableString(fURL) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
