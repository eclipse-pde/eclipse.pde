/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.ILicenseInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class LicenseInfo extends ProductObject implements ILicenseInfo {

	public static final String P_URL = "url"; //$NON-NLS-1$
	public static final String P_LICENSE = "text"; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;

	private String fURL;
	private String fLicense;

	public LicenseInfo(IProductModel model) {
		super(model);
	}

	@Override
	public void setURL(String url) {
		String old = fURL;
		fURL = url;
		if (isEditable()) {
			firePropertyChanged(P_URL, old, fURL);
		}
	}

	@Override
	public String getURL() {
		return fURL;
	}

	@Override
	public String getLicense() {
		return fLicense;
	}

	@Override
	public void setLicense(String text) {
		String old = fLicense;
		fLicense = text;
		if (isEditable()) {
			firePropertyChanged(P_LICENSE, old, fLicense);
		}
	}

	@Override
	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(P_LICENSE)) {
					child.normalize();
					if (child.getChildNodes().getLength() > 0) {
						Node text = child.getFirstChild();
						if (text.getNodeType() == Node.TEXT_NODE) {
							fLicense = ((Text) text).getData().trim();
						}
					}
				}
				if (child.getNodeName().equals(P_URL)) {
					child.normalize();
					if (child.getChildNodes().getLength() > 0) {
						Node text = child.getFirstChild();
						if (text.getNodeType() == Node.TEXT_NODE) {
							fURL = ((Text) text).getData().trim();
						}
					}
				}
			}
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (isURLDefined() || isLicenseTextDefined()) {
			writer.println(indent + "<license>"); //$NON-NLS-1$
			if (isURLDefined()) {
				writer.println(indent + "     <url>" + getWritableString(fURL.trim()) + "</url>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (isLicenseTextDefined()) {
				writer.println(indent + "     <text>"); //$NON-NLS-1$
				writer.println(indent + getWritableString(fLicense.trim()));
				writer.println(indent + "      </text>"); //$NON-NLS-1$
			}
			writer.println(indent + "</license>"); //$NON-NLS-1$
		}

	}

	private boolean isURLDefined() {
		return fURL != null && fURL.length() > 0;
	}

	private boolean isLicenseTextDefined() {
		return fLicense != null && fLicense.length() > 0;
	}

}
