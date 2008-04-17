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
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.*;

public class ConfigurationFileInfo extends ProductObject implements IConfigurationFileInfo {

	private static final long serialVersionUID = 1L;

	private String fUse;
	private String fPath;

	private static final String LIN = Constants.OS_LINUX;
	private static final String MAC = Constants.OS_MACOSX;
	private static final String SOL = Constants.OS_SOLARIS;
	private static final String WIN = Constants.OS_WIN32;

	private String fLinPath, fLinUse;
	private String fMacPath, fMacUse;
	private String fSolPath, fSolUse;
	private String fWinPath, fWinUse;

	public ConfigurationFileInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		String old = fPath;
		fPath = path;
		if (isEditable())
			firePropertyChanged(P_PATH, old, fPath);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getPath()
	 */
	public String getPath() {
		return fPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fPath = element.getAttribute(P_PATH);
			fUse = element.getAttribute(P_USE);
			NodeList list = element.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (child.getNodeName().equals(LIN)) {
						fLinPath = getText(child);
						fLinUse = fLinPath == null ? "default" : "custom"; //$NON-NLS-1$ //$NON-NLS-2$
					} else if (child.getNodeName().equals(MAC)) {
						fMacPath = getText(child);
						fMacUse = fMacPath == null ? "default" : "custom"; //$NON-NLS-1$ //$NON-NLS-2$
					} else if (child.getNodeName().equals(SOL)) {
						fSolPath = getText(child);
						fSolUse = fSolPath == null ? "default" : "custom"; //$NON-NLS-1$ //$NON-NLS-2$
					} else if (child.getNodeName().equals(WIN)) {
						fWinPath = getText(child);
						fWinUse = fWinPath == null ? "default" : "custom"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			// for backwards compatibility
			// if we have an old path, we convert it to a platform specific path if it wasn't set
			if (fPath != null && fUse.equals("custom")) { //$NON-NLS-1$
				if (fLinUse == null) {
					fLinPath = fLinPath == null ? fPath : null;
					fLinUse = "custom"; //$NON-NLS-1$
				}
				if (fMacUse == null) {
					fMacPath = fMacPath == null ? fPath : null;
					fMacUse = "custom"; //$NON-NLS-1$
				}
				if (fSolUse == null) {
					fSolPath = fSolPath == null ? fPath : null;
					fSolUse = "custom"; //$NON-NLS-1$
				}
				if (fWinUse == null) {
					fWinPath = fWinPath == null ? fPath : null;
					fWinUse = "custom"; //$NON-NLS-1$
				}
				// null out things
				fPath = null;
				fUse = "default"; //$NON-NLS-1$
			}
		}
	}

	private String getText(Node node) {
		node.normalize();
		Node text = node.getFirstChild();
		if (text != null && text.getNodeType() == Node.TEXT_NODE) {
			return text.getNodeValue();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		// the first entry here is for backwards compatibility
		writer.print(indent + "<configIni"); //$NON-NLS-1$
		if (fUse != null)
			writer.print(" " + P_USE + "=\"" + fUse + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fPath != null && fPath.trim().length() > 0)
			writer.print(" " + P_PATH + "=\"" + getWritableString(fPath.trim()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		writer.println(">"); //$NON-NLS-1$

		// write out the platform specific config.ini entries
		if (fLinPath != null) {
			writer.print(indent);
			writer.print("   <" + LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(getWritableString(fLinPath.trim()));
			writer.println("</" + LIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (fMacPath != null) {
			writer.print(indent);
			writer.print("   <" + MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(getWritableString(fMacPath.trim()));
			writer.println("</" + MAC + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (fSolPath != null) {
			writer.print(indent);
			writer.print("   <" + SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(getWritableString(fSolPath.trim()));
			writer.println("</" + SOL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (fWinPath != null) {
			writer.print(indent);
			writer.print("   <" + WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(getWritableString(fWinPath.trim()));
			writer.println("</" + WIN + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		writer.print(indent + "</configIni>"); //$NON-NLS-1$
		writer.println();
	}

	public void setUse(String os, String use) {
		if (os == null) {
			String old = fUse;
			fUse = use;
			if (isEditable())
				firePropertyChanged(P_USE, old, fUse);
		}

		if (Platform.OS_WIN32.equals(os)) {
			String old = fWinUse;
			fWinUse = use;
			if (isEditable())
				firePropertyChanged(WIN, old, fWinUse);
		} else if (Platform.OS_LINUX.equals(os)) {
			String old = fLinUse;
			fLinUse = use;
			if (isEditable())
				firePropertyChanged(LIN, old, fLinUse);
		} else if (Platform.OS_MACOSX.equals(os)) {
			String old = fMacUse;
			fMacUse = use;
			if (isEditable())
				firePropertyChanged(MAC, old, fMacUse);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			String old = fSolUse;
			fSolUse = use;
			if (isEditable())
				firePropertyChanged(SOL, old, fSolUse);
		}
	}

	public String getUse(String os) {
		if (os == null)
			return fUse;

		if (Platform.OS_WIN32.equals(os)) {
			return fWinUse;
		} else if (Platform.OS_LINUX.equals(os)) {
			return fLinUse;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return fMacUse;
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return fSolUse;
		}
		return null;
	}

	public void setPath(String os, String path) {
		if (os == null) {
			String old = fPath;
			fPath = path;
			if (isEditable())
				firePropertyChanged(P_PATH, old, fPath);
		}

		if (Platform.OS_WIN32.equals(os)) {
			String old = fWinPath;
			fWinPath = path;
			if (isEditable())
				firePropertyChanged(WIN, old, fWinPath);
		} else if (Platform.OS_LINUX.equals(os)) {
			String old = fLinPath;
			fLinPath = path;
			if (isEditable())
				firePropertyChanged(LIN, old, fLinPath);
		} else if (Platform.OS_MACOSX.equals(os)) {
			String old = fMacPath;
			fMacPath = path;
			if (isEditable())
				firePropertyChanged(MAC, old, fMacPath);
		} else if (Platform.OS_SOLARIS.equals(os)) {
			String old = fSolPath;
			fSolPath = path;
			if (isEditable())
				firePropertyChanged(SOL, old, fSolPath);
		}
	}

	public String getPath(String os) {
		if (os == null)
			return fPath;

		if (Platform.OS_WIN32.equals(os)) {
			return fWinPath;
		} else if (Platform.OS_LINUX.equals(os)) {
			return fLinPath;
		} else if (Platform.OS_MACOSX.equals(os)) {
			return fMacPath;
		} else if (Platform.OS_SOLARIS.equals(os)) {
			return fSolPath;
		}
		return null;
	}

}
