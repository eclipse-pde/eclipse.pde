/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.File;
import java.io.PrintWriter;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JREInfo extends ProductObject implements IJREInfo {

	private static final String JRE_LIN = "linux"; //$NON-NLS-1$
	private static final String JRE_MAC = "macos"; //$NON-NLS-1$
	private static final String JRE_SOL = "solaris"; //$NON-NLS-1$
	private static final String JRE_WIN = "windows"; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;
	private IPath fJVMLin;
	private IPath fJVMMac;
	private IPath fJVMSol;
	private IPath fJVMWin;

	public JREInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IJREInfo#getJREContainerPath(java.lang.String)
	 */
	public IPath getJREContainerPath(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return fJVMWin;
		} else if (Platform.OS_LINUX.equals(os)) {
			return fJVMLin;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return fJVMMac;
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return fJVMSol;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IJREInfo#setJREContainerPath(java.lang.String, java.lang.String)
	 */
	public void setJREContainerPath(String os, IPath jreContainerPath) {
		if (Platform.OS_WIN32.equals(os)) {
			IPath old = fJVMWin;
			fJVMWin = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
		} else if (Platform.OS_LINUX.equals(os)) {
			IPath old = fJVMLin;
			fJVMLin = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
		} else if (Platform.OS_MACOSX.equals(os)) {
			IPath old = fJVMMac;
			fJVMMac = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			IPath old = fJVMSol;
			fJVMSol = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IJREInfo#getJVMLocation(java.lang.String)
	 */
	public File getJVMLocation(String os) {
		IPath jreContainerPath = getJREContainerPath(os);
		if (jreContainerPath == null) // no vm was specified
			return null;
		IVMInstall vm = JavaRuntime.getVMInstall(jreContainerPath);
		if (vm != null) {
			return vm.getInstallLocation();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(JRE_LIN)) {
					fJVMLin = getPath(child);
				} else if (child.getNodeName().equals(JRE_MAC)) {
					fJVMMac = getPath(child);
				} else if (child.getNodeName().equals(JRE_SOL)) {
					fJVMSol = getPath(child);
				} else if (child.getNodeName().equals(JRE_WIN)) {
					fJVMWin = getPath(child);
				}
			}
		}
	}

	/**
	 * Gets the text out of the node and attempts to create an IPath from it.
	 * @param node node to extract the path from
	 * @return a path created from the node's text or <code>null</code>
	 */
	private IPath getPath(Node node) {
		node.normalize();
		Node text = node.getFirstChild();
		if (text != null && text.getNodeType() == Node.TEXT_NODE) {
			String pathString = text.getNodeValue();
			if (pathString != null && pathString.length() > 0) {
				return new Path(pathString);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<vm>"); //$NON-NLS-1$
		if (fJVMLin != null) {
			writer.print(indent);
			writer.print("   <" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(fJVMLin.toPortableString());
			writer.println("</" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMMac != null) {
			writer.print(indent);
			writer.print("   <" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(fJVMMac.toPortableString());
			writer.println("</" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMSol != null) {
			writer.print(indent);
			writer.print("   <" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(fJVMSol.toPortableString());
			writer.println("</" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMWin != null) {
			writer.print(indent);
			writer.print("   <" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(fJVMWin.toPortableString());
			writer.println("</" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

}
