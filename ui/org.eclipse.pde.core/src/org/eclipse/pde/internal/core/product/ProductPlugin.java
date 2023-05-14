/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 264462
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
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

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fVersion = element.getAttribute("version"); //$NON-NLS-1$
			String fragment = element.getAttribute("fragment"); //$NON-NLS-1$
			fFragment = Boolean.parseBoolean(fragment);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fVersion != null && fVersion.length() > 0 && !fVersion.equals(ICoreConstants.DEFAULT_VERSION)) {
			writer.print(" version=\"" + fVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// If the plugin is a known fragment or has a cached fragment setting, mark it as a fragment
		if (PluginRegistry.findModel(fId) != null) {
			if (PluginRegistry.findModel(fId) instanceof IFragmentModel) {
				writer.print(" fragment=\"" + Boolean.TRUE + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (fFragment) {
			writer.print(" fragment=\"" + Boolean.TRUE + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return fId.trim();
	}

	@Override
	public void setId(String id) {
		fId = id;
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	@Override
	public void setVersion(String version) {
		String old = fVersion;
		fVersion = version;
		if (isEditable()) {
			firePropertyChanged("version", old, fVersion); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isFragment() {
		return fFragment;
	}

	@Override
	public void setFragment(boolean isFragment) {
		fFragment = isFragment;
	}

}
