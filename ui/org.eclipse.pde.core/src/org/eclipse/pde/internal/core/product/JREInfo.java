/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JREInfo extends ProductObject implements IJREInfo {

	private static final String JRE_FBSD = "freebsd"; //$NON-NLS-1$
	private static final String JRE_LIN = "linux"; //$NON-NLS-1$
	private static final String JRE_MAC = "macos"; //$NON-NLS-1$
	private static final String JRE_SOL = "solaris"; //$NON-NLS-1$
	private static final String JRE_WIN = "windows"; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;
	private IPath fJVMFbsd;
	private IPath fJVMLin;
	private IPath fJVMMac;
	private IPath fJVMSol;
	private IPath fJVMWin;

	private boolean bIncludeFbsd;
	private boolean bIncludeLin;
	private boolean bIncludeMac;
	private boolean bIncludeSol;
	private boolean bIncludeWin;

	public JREInfo(IProductModel model) {
		super(model);
	}

	@Override
	public IPath getJREContainerPath(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return fJVMWin;
		} else if (Platform.OS_FREEBSD.equals(os)) {
			return fJVMFbsd;
		} else if (Platform.OS_LINUX.equals(os)) {
			return fJVMLin;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return fJVMMac;
		}
		return null;
	}

	@Override
	public void setJREContainerPath(String os, IPath jreContainerPath) {
		if (Platform.OS_WIN32.equals(os)) {
			IPath old = fJVMWin;
			fJVMWin = jreContainerPath;
			if (isEditable()) {
				firePropertyChanged(JRE_WIN, old, fJVMWin);
			}
		} else if (Platform.OS_FREEBSD.equals(os)) {
			IPath old = fJVMFbsd;
			fJVMFbsd = jreContainerPath;
			if (isEditable()) {
				firePropertyChanged(JRE_FBSD, old, fJVMFbsd);
			}
		} else if (Platform.OS_LINUX.equals(os)) {
			IPath old = fJVMLin;
			fJVMLin = jreContainerPath;
			if (isEditable()) {
				firePropertyChanged(JRE_LIN, old, fJVMLin);
			}
		} else if (Platform.OS_MACOSX.equals(os)) {
			IPath old = fJVMMac;
			fJVMMac = jreContainerPath;
			if (isEditable()) {
				firePropertyChanged(JRE_MAC, old, fJVMMac);
			}
		}
	}

	@Override
	public File getJVMLocation(String os) {
		IPath jreContainerPath = getJREContainerPath(os);
		if (jreContainerPath == null) {
			return null;
		}
		IVMInstall vm = JavaRuntime.getVMInstall(jreContainerPath);
		if (vm != null) {
			return vm.getInstallLocation();
		}
		return null;
	}

	@Override
	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Node includeNode = child.getAttributes().getNamedItem("include"); //$NON-NLS-1$
				boolean include = includeNode != null ? Boolean.parseBoolean(includeNode.getNodeValue()) : true;
				switch (child.getNodeName()) {
				case JRE_FBSD:
					fJVMFbsd = getPath(child);
					bIncludeFbsd = include;
					break;
				case JRE_LIN:
					fJVMLin = getPath(child);
					bIncludeLin = include;
					break;
				case JRE_MAC:
					fJVMMac = getPath(child);
					bIncludeMac = include;
					break;
				case JRE_SOL:
					fJVMSol = getPath(child);
					bIncludeSol = include;
					break;
				case JRE_WIN:
					fJVMWin = getPath(child);
					bIncludeWin = include;
					break;
				default:
					break;
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
				return IPath.fromOSString(pathString);
			}
		}
		return null;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<vm>"); //$NON-NLS-1$
		if (fJVMFbsd != null) {
			writer.print(indent);
			writer.print("   <" + JRE_FBSD + " include=\"" + bIncludeFbsd + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMFbsd.toPortableString());
			writer.println("</" + JRE_FBSD + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMLin != null) {
			writer.print(indent);
			writer.print("   <" + JRE_LIN + " include=\"" + bIncludeLin + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMLin.toPortableString());
			writer.println("</" + JRE_LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMMac != null) {
			writer.print(indent);
			writer.print("   <" + JRE_MAC + " include=\"" + bIncludeMac + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMMac.toPortableString());
			writer.println("</" + JRE_MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMSol != null) {
			writer.print(indent);
			writer.print("   <" + JRE_SOL + " include=\"" + bIncludeSol + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMSol.toPortableString());
			writer.println("</" + JRE_SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fJVMWin != null) {
			writer.print(indent);
			writer.print("   <" + JRE_WIN + " include=\"" + bIncludeWin + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.print(fJVMWin.toPortableString());
			writer.println("</" + JRE_WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(indent + "</vm>"); //$NON-NLS-1$
	}

	@Override
	public boolean includeJREWithProduct(String os) {
		if (Platform.OS_WIN32.equals(os)) {
			return bIncludeWin;
		} else if (Platform.OS_FREEBSD.equals(os)) {
			return bIncludeFbsd;
		} else if (Platform.OS_LINUX.equals(os)) {
			return bIncludeLin;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return bIncludeMac;
		}
		return false;
	}

	@Override
	public void setIncludeJREWithProduct(String os, boolean includeJRE) {
		if (Platform.OS_WIN32.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeWin);
			bIncludeWin = includeJRE;
			if (isEditable()) {
				firePropertyChanged(JRE_WIN, old, Boolean.valueOf(bIncludeWin));
			}
		} else if (Platform.OS_FREEBSD.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeFbsd);
			bIncludeFbsd = includeJRE;
			if (isEditable()) {
				firePropertyChanged(JRE_FBSD, old, Boolean.valueOf(bIncludeFbsd));
			}
		} else if (Platform.OS_LINUX.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeLin);
			bIncludeLin = includeJRE;
			if (isEditable()) {
				firePropertyChanged(JRE_LIN, old, Boolean.valueOf(bIncludeLin));
			}
		} else if (Platform.OS_MACOSX.equals(os)) {
			Boolean old = Boolean.valueOf(bIncludeMac);
			bIncludeMac = includeJRE;
			if (isEditable()) {
				firePropertyChanged(JRE_MAC, old, Boolean.valueOf(bIncludeMac));
			}
		}
	}

}
