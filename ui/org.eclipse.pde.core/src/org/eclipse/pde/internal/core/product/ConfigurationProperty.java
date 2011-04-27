/*******************************************************************************
 * Copyright (c) 2009, 2011 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM Corporation - additional enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IConfigurationProperty;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ConfigurationProperty extends ProductObject implements IConfigurationProperty {

	private static final long serialVersionUID = -3549668957352554826L;
	private String fName;
	private String fValue;

	/**
	 * Only for parsing usage
	 * @param model
	 */
	ConfigurationProperty(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fName = element.getAttribute("name"); //$NON-NLS-1$
			fValue = element.getAttribute("value"); //$NON-NLS-1$
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<property name=\"" + fName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" value=\"" + fValue + "\""); //$NON-NLS-1$//$NON-NLS-2$
		writer.println(" />"); //$NON-NLS-1$
	}

	public String getName() {
		return fName;
	}

	public String getValue() {
		return fValue;
	}

	public void setName(String name) {
		String oldValue = fName;
		fName = name;
		if (isEditable() && !fName.equals(oldValue)) {
			firePropertyChanged(P_NAME, oldValue, fName);
		}
	}

	public void setValue(String value) {
		String oldValue = fValue;
		fValue = value;
		if (isEditable() && !fValue.equals(oldValue)) {
			firePropertyChanged(P_VALUE, oldValue, fValue);
		}
	}

	public String toString() {
		return fName + " : " + fValue; //$NON-NLS-1$
	}

}
