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
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProductFeature extends ProductObject implements IProductFeature {

	/**
	 * the models version property
	 */
	public static final String PROP_VERSION = "version"; //$NON-NLS-1$

	/**
	 * the models root install mode property
	 */
	public static final String PROP_INSTALL_MODE_ROOT = "installmoderoot"; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fVersion;

	private boolean fInstallModeRoot;

	public ProductFeature(IProductModel model) {
		super(model);
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fVersion = element.getAttribute("version"); //$NON-NLS-1$
			fInstallModeRoot = "root".equals(element.getAttribute("installMode")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<feature id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fVersion != null && fVersion.length() > 0 && !fVersion.equals(ICoreConstants.DEFAULT_VERSION)) {
			writer.print(" version=\"" + fVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fInstallModeRoot) {
			writer.print(" installMode=\"root\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return fId;
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
			firePropertyChanged(PROP_VERSION, old, fVersion);
		}
	}

	@Override
	public IProductFeature setRootInstallMode(boolean root) {
		boolean old = fInstallModeRoot;
		fInstallModeRoot = root;
		if (isEditable()) {
			firePropertyChanged(PROP_INSTALL_MODE_ROOT, old, fInstallModeRoot);
		}
		return this;
	}

	@Override
	public boolean isRootInstallMode() {
		return fInstallModeRoot;
	}
}
