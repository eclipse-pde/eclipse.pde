/*******************************************************************************
 *  Copyright (c) 2024 SAP SE and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.iproduct.IMacBundleUrlType;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MacBundleUrlType extends ProductObject implements IMacBundleUrlType {

	private static final long serialVersionUID = 1L;
	private String fScheme;
	private String fName;

	public MacBundleUrlType(IProductModel model) {
		super(model);
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fScheme = element.getAttribute("scheme"); //$NON-NLS-1$
			fName = element.getAttribute("name"); //$NON-NLS-1$
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<bundleUrlType scheme=\"" + fScheme + "\"" + " name=\"" + fName + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}


	@Override
	public String getScheme() {
		return fScheme;
	}

	@Override
	public void setScheme(String scheme) {
		fScheme = scheme;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public void setName(String name) {
		fName = name;
	}

}
