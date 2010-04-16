/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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

	private boolean bIncludeLin;
	private boolean bIncludeMac;
	private boolean bIncludeSol;
	private boolean bIncludeWin;

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
				firePropertyChanged(JRE_WIN, old, fJVMWin);
		} else if (Platform.OS_LINUX.equals(os)) {
			IPath old = fJVMLin;
			fJVMLin = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, fJVMLin);
		} else if (Platform.OS_MACOSX.equals(os)) {
			IPath old = fJVMMac;
			fJVMMac = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_MAC, old, fJVMMac);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			IPath old = fJVMSol;
			fJVMSol = jreContainerPath;
			if (isEditable())
				firePropertyChanged(JRE_SOL, old, fJVMSol);
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
				Node includeNode = child.getAttributes().getNamedItem("include"); //$NON-NLS-1$
				boolean include = includeNode != null ? Boolean.valueOf(includeNode.getNodeValue()).booleanValue() : true;
				if (child.getNodeName().equals(JRE_LIN)) {
					fJVMLin = getPath(child);
					bIncludeLin = include;
				} else if (child.getNodeName().equals(JRE_MAC)) {
					fJVMMac = getPath(child);
					bIncludeMac = include;
				} else if (child.getNodeName().equals(JRE_SOL)) {
					fJVMSol = getPath(child);
					bIncludeSol = include;
				} else if (child.getNodeName().equals(JRE_WIN)) {
					fJVMWin = getPath(child);
					bIncludeWin = include;
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
			writer.print("   <" + JRE_LIN + " include=\"" + String.valueOf(bIncludeLin) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMLin.toPortableString());
			writer.println("</" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMMac != null) {
			writer.print(indent);
			writer.print("   <" + JRE_MAC + " include=\"" + String.valueOf(bIncludeMac) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMMac.toPortableString());
			writer.println("</" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMSol != null) {
			writer.print(indent);
			writer.print("   <" + JRE_SOL + " include=\"" + String.valueOf(bIncludeSol) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMSol.toPortableString());
			writer.println("</" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMWin != null) {
			writer.print(indent);
			writer.print("   <" + JRE_WIN + " include=\"" + String.valueOf(bIncludeWin) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMWin.toPortableString());
			writer.println("</" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

	public boolean includeJREWithProduct(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return bIncludeWin;
		} else if (Platform.OS_LINUX.equals(os)) {
			return bIncludeLin;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return bIncludeMac;
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return bIncludeSol;
		}
		return false;
	}

	public void setIncludeJREWithProduct(String os, boolean includeJRE) {
		if (Platform.OS_WIN32.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeWin);
			bIncludeWin = includeJRE;
			if (isEditable())
				firePropertyChanged(JRE_WIN, old, Boolean.valueOf(bIncludeWin));
		} else if (Platform.OS_LINUX.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeLin);
			bIncludeLin = includeJRE;
			if (isEditable())
				firePropertyChanged(JRE_LIN, old, Boolean.valueOf(bIncludeLin));
		} else if (Platform.OS_MACOSX.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeMac);
			bIncludeMac = includeJRE;
			if (isEditable())
				firePropertyChanged(JRE_MAC, old, Boolean.valueOf(bIncludeMac));
		} else if (Platform.OS_SOLARIS.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeSol);
			bIncludeSol = includeJRE;
			if (isEditable())
				firePropertyChanged(JRE_SOL, old, Boolean.valueOf(bIncludeSol));
		}
	}

}
