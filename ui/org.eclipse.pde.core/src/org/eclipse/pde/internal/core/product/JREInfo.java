/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JREInfo extends ProductObject implements IJREInfo {

	private static final long serialVersionUID = 1L;
	private String fJVMLin = ""; //$NON-NLS-1$
	private String fJVMMac = ""; //$NON-NLS-1$
	private String fJVMSol = ""; //$NON-NLS-1$
	private String fJVMWin = ""; //$NON-NLS-1$

	public JREInfo(IProductModel model) {
		super(model);
	}

	public String getJVM(int platform) {
		switch (platform) {
		case LINUX:
			return fJVMLin;
		case MACOS:
			return fJVMMac;
		case SOLAR:
			return fJVMSol;
		case WIN32:
			return fJVMWin;
		}
		return ""; //$NON-NLS-1$
	}

	public void setJVM(String args, int platform) {
		String old;
		if (args == null)
			args = ""; //$NON-NLS-1$
		switch (platform) {
		case LINUX:
			old = fJVMLin;
			fJVMLin = args;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
			break;
		case MACOS:
			old = fJVMMac;
			fJVMMac = args;
			if (isEditable())
				firePropertyChanged(JRE_MAC, old, fJVMMac);
			break;
		case SOLAR:
			old = fJVMSol;
			fJVMSol = args;
			if (isEditable())
				firePropertyChanged(JRE_SOL, old, fJVMSol);
			break;
		case WIN32:
			old = fJVMWin;
			fJVMWin = args;
			if (isEditable())
				firePropertyChanged(JRE_WIN, old, fJVMWin);
			break;
		}
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(JRE_LIN)) {
					fJVMLin = getText(child);
				} else if (child.getNodeName().equals(JRE_MAC)) {
					fJVMMac = getText(child);
				} else if (child.getNodeName().equals(JRE_SOL)) {
					fJVMSol = getText(child);
				} else if (child.getNodeName().equals(JRE_WIN)) {
					fJVMWin = getText(child);
				}
			}
		}
	}
	
	private String getText(Node node) {
		node.normalize();
		Node text = node.getFirstChild();
		if (text != null && text.getNodeType() == Node.TEXT_NODE) {
			return text.getNodeValue();
		}
		return ""; //$NON-NLS-1$
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<vm>"); //$NON-NLS-1$
		if (fJVMLin.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_LIN + ">" + getWritableString(fJVMLin) + "</" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMMac.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_MAC + ">" + getWritableString(fJVMMac) + "</" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMSol.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_SOL + ">" + getWritableString(fJVMSol) + "</" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMWin.length() > 0) {
			writer.println(indent + "   " + "<" + JRE_WIN + ">" + getWritableString(fJVMWin) + "</" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

}
