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

import org.eclipse.pde.internal.core.iproduct.IJVMInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JVMInfo extends ProductObject implements IJVMInfo {

	private static final long serialVersionUID = 1L;
	private String fJVMLin = ""; //$NON-NLS-1$
	private String fJVMMac = ""; //$NON-NLS-1$
	private String fJVMSol = ""; //$NON-NLS-1$
	private String fJVMWin = ""; //$NON-NLS-1$

	public JVMInfo(IProductModel model) {
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
				firePropertyChanged(JVM_LIN, old, fJVMLin);
			break;
		case MACOS:
			old = fJVMMac;
			fJVMMac = args;
			if (isEditable())
				firePropertyChanged(JVM_MAC, old, fJVMMac);
			break;
		case SOLAR:
			old = fJVMSol;
			fJVMSol = args;
			if (isEditable())
				firePropertyChanged(JVM_SOL, old, fJVMSol);
			break;
		case WIN32:
			old = fJVMWin;
			fJVMWin = args;
			if (isEditable())
				firePropertyChanged(JVM_WIN, old, fJVMWin);
			break;
		}
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(JVM_LIN)) {
					fJVMLin = getText(child);
				} else if (child.getNodeName().equals(JVM_MAC)) {
					fJVMMac = getText(child);
				} else if (child.getNodeName().equals(JVM_SOL)) {
					fJVMSol = getText(child);
				} else if (child.getNodeName().equals(JVM_WIN)) {
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
			writer.println(indent + "   " + "<" + JVM_LIN + ">" + getWritableString(fJVMLin) + "</" + JVM_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMMac.length() > 0) {
			writer.println(indent + "   " + "<" + JVM_MAC + ">" + getWritableString(fJVMMac) + "</" + JVM_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMSol.length() > 0) {
			writer.println(indent + "   " + "<" + JVM_SOL + ">" + getWritableString(fJVMSol) + "</" + JVM_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fJVMWin.length() > 0) {
			writer.println(indent + "   " + "<" + JVM_WIN + ">" + getWritableString(fJVMWin) + "</" + JVM_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

}
