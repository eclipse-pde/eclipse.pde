/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 264462
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProductPlugin extends ProductObject implements IProductPlugin {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fVersion;

	/**
	 * Used to cache the fragment attribute value internally in order to not lose it in case the current 
	 * plugin/fragment is not in the target platform anymore (see bug 264462)
	 */
	private boolean fFragment;

	public ProductPlugin(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fVersion = element.getAttribute("version"); //$NON-NLS-1$
			String fragment = element.getAttribute("fragment"); //$NON-NLS-1$
			fFragment = Boolean.valueOf(fragment).booleanValue();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fVersion != null && fVersion.length() > 0 && !fVersion.equals("0.0.0")) { //$NON-NLS-1$
			writer.print(" version=\"" + fVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// If the plugin is a known fragment or has a cached fragment setting, mark it as a fragment 		
		if (PluginRegistry.findModel(fId) != null) {
			if (PluginRegistry.findModel(fId) instanceof IFragmentModel) {
				writer.print(" fragment=\"" + Boolean.TRUE.toString() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (fFragment) {
			writer.print(" fragment=\"" + Boolean.TRUE.toString() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		writer.println("/>"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#getId()
	 */
	public String getId() {
		return fId.trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId = id;
	}

	public String getVersion() {
		return fVersion;
	}

	public void setVersion(String version) {
		String old = fVersion;
		fVersion = version;
		if (isEditable())
			firePropertyChanged("version", old, fVersion); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#isFragment()
	 */
	public boolean isFragment() {
		return fFragment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPlugin#setFragment(boolean)
	 */
	public void setFragment(boolean isFragment) {
		fFragment = isFragment;
	}

}
