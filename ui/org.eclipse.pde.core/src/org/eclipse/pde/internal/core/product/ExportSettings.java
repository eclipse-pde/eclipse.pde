/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.iproduct.IExportSettings;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ExportSettings extends ProductObject implements
		IExportSettings {

	private static final long serialVersionUID = 1L;
	
	private String fLastRoot;
	private String fLastDest;
	private String fDirectoryDest;
	
	public ExportSettings(IProductModel model) {
		super(model);
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fLastDest = element.getAttribute(P_LAST_DEST);
			fLastRoot = element.getAttribute(P_LAST_ROOT);
			fDirectoryDest = element.getAttribute(P_DIRECTORY_DEST);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<exportSettings"); //$NON-NLS-1$
		if (fLastDest != null && fLastDest.trim().length() > 0)
			writer.print(" "  + P_LAST_DEST + "=\"" + getWritableString(fLastDest.trim()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fLastRoot != null && fLastRoot.trim().length() > 0)
			writer.print(" "  + P_LAST_ROOT + "=\"" + getWritableString(fLastRoot.trim()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.print(" "  + P_DIRECTORY_DEST + "=\"" + fDirectoryDest + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println("/>"); //$NON-NLS-1$
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getLastDest()
	 */
	public String getLastDest() {
		return fLastDest;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setLastDest(java.lang.String)
	 */
	public void setLastDest(String lastDest) {
		String old = fLastDest;
		fLastDest = lastDest;
		if (isEditable())
			firePropertyChanged(P_LAST_DEST, old, fLastDest);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#getLastRoot()
	 */
	public String getLastRoot() {
		return fLastRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setLastRoot(java.lang.String)
	 */
	public void setLastRoot(String lastRoot) {
		String old = fLastRoot;
		fLastRoot = lastRoot;
		if (isEditable())
			firePropertyChanged(P_LAST_ROOT, old, fLastRoot);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#isDirectory()
	 */
	public boolean isDirectory() {
		return Boolean.toString(true).equals(fDirectoryDest);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo#setIsDirectory(boolean)
	 */
	public void setIsDirectory(boolean isDir) {
		String old = fDirectoryDest;
		fDirectoryDest = Boolean.toString(isDir);
		if (isEditable())
			firePropertyChanged(P_LAST_ROOT, old, fDirectoryDest);
	}
	
}
