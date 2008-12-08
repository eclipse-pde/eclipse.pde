/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProductFeature extends ProductObject implements IProductFeature {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fVersion;

	public ProductFeature(IProductModel model) {
		super(model);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fVersion = element.getAttribute("version"); //$NON-NLS-1$
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<feature id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fVersion != null && fVersion.length() > 0 && !fVersion.equals("0.0.0")) { //$NON-NLS-1$
			writer.print(" version=\"" + fVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	public String getId() {
		return fId;
	}

	public void setId(String id) {
		fId = id;
	}

	public String getVersion() {
		return fVersion;
	}

	public void setVersion(String version) {
		String old = fVersion;
		fVersion = version;
		if (isEditable())
			firePropertyChanged("version", old, fVersion); //$NON-NLS-1$
	}

}
