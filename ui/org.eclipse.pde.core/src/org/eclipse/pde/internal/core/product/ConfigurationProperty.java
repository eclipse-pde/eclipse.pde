/*******************************************************************************
 * Copyright (c) 2009, 2014 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	private String fOS;
	private String fArch;

	/**
	 * Only for parsing usage
	 * @param model
	 */
	ConfigurationProperty(IProductModel model) {
		super(model);
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fName = element.getAttribute("name"); //$NON-NLS-1$
			fValue = element.getAttribute("value"); //$NON-NLS-1$
			fOS = element.getAttribute("os"); //$NON-NLS-1$
			fArch = element.getAttribute("arch"); //$NON-NLS-1$
		}

	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<property name=\"" + fName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" value=\"" + fValue + "\""); //$NON-NLS-1$//$NON-NLS-2$
		if (fOS.length() > 0) {
			writer.print(" os=\"" + fOS + "\""); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (fArch.length() > 0) {
			writer.print(" arch=\"" + fArch + "\""); //$NON-NLS-1$//$NON-NLS-2$
		}
		writer.println(" />"); //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getValue() {
		return fValue;
	}

	@Override
	public void setName(String name) {
		String oldValue = fName;
		fName = name;
		if (isEditable() && !fName.equals(oldValue)) {
			firePropertyChanged(P_NAME, oldValue, fName);
		}
	}

	@Override
	public void setValue(String value) {
		String oldValue = fValue;
		fValue = value;
		if (isEditable() && !fValue.equals(oldValue)) {
			firePropertyChanged(P_VALUE, oldValue, fValue);
		}
	}

	@Override
	public String toString() {
		return fName + " : " + fValue; //$NON-NLS-1$
	}

	@Override
	public String getOs() {
		return fOS;
	}

	@Override
	public void setOs(String os) {
		String oldValue = fOS;
		fOS = os;
		if (isEditable() && !fOS.equals(oldValue)) {
			firePropertyChanged(P_OS, oldValue, fOS);
		}
	}

	@Override
	public String getArch() {
		return fArch;
	}

	@Override
	public void setArch(String arch) {
		String oldValue = fArch;
		fArch = arch;
		if (isEditable() && !fArch.equals(oldValue)) {
			firePropertyChanged(P_ARCH, oldValue, fArch);
		}
	}
}
