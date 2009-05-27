/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class AboutInfo extends ProductObject implements IAboutInfo {

	private static final long serialVersionUID = 1L;
	private String fImagePath;
	private String fAboutText;

	public AboutInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#setText(java.lang.String)
	 */
	public void setText(String text) {
		String old = fAboutText;
		fAboutText = text;
		if (isEditable())
			firePropertyChanged(P_TEXT, old, fAboutText);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#getText()
	 */
	public String getText() {
		return fAboutText;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#setImagePath(java.lang.String)
	 */
	public void setImagePath(String path) {
		String old = fImagePath;
		fImagePath = path;
		if (isEditable())
			firePropertyChanged(P_IMAGE, old, fImagePath);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IAboutInfo#getImagePath()
	 */
	public String getImagePath() {
		return fImagePath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		if (isAboutImageDefined() || isAboutTextDefined()) {
			writer.println(indent + "<aboutInfo>"); //$NON-NLS-1$
			if (isAboutImageDefined())
				writer.println(indent + "   <image path=\"" + getWritableString(fImagePath.trim()) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			if (isAboutTextDefined()) {
				writer.println(indent + "   <text>"); //$NON-NLS-1$
				writer.println(indent + "      " + getWritableString(fAboutText.trim())); //$NON-NLS-1$
				writer.println(indent + "   </text>"); //$NON-NLS-1$
			}
			writer.println(indent + "</aboutInfo>"); //$NON-NLS-1$
		}
	}

	private boolean isAboutTextDefined() {
		return fAboutText != null && fAboutText.length() > 0;
	}

	private boolean isAboutImageDefined() {
		return fImagePath != null && fImagePath.length() > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("image")) { //$NON-NLS-1$
					fImagePath = ((Element) child).getAttribute("path"); //$NON-NLS-1$
				} else if (child.getNodeName().equals("text")) { //$NON-NLS-1$
					child.normalize();
					if (child.getChildNodes().getLength() > 0) {
						Node text = child.getFirstChild();
						if (text.getNodeType() == Node.TEXT_NODE)
							fAboutText = ((Text) text).getData().trim();
					}
				}
			}
		}
	}

}
