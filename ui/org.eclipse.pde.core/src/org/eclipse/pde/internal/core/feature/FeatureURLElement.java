/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.w3c.dom.Node;

public class FeatureURLElement extends FeatureObject implements IFeatureURLElement {
	private static final long serialVersionUID = 1L;
	private final int fElementType;
	private int fSiteType = UPDATE_SITE;
	private URL fUrl;

	public FeatureURLElement(int elementType) {
		this.fElementType = elementType;
	}

	public FeatureURLElement(int elementType, URL url) {
		this.fElementType = elementType;
		this.fUrl = url;
	}

	@Override
	public int getElementType() {
		return fElementType;
	}

	@Override
	public URL getURL() {
		return fUrl;
	}

	@Override
	public int getSiteType() {
		return fSiteType;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		String urlName = getNodeAttribute(node, "url"); //$NON-NLS-1$
		try {
			if (urlName != null) {
				fUrl = new URL(urlName);
			}
		} catch (MalformedURLException e) {
		}
		String typeName = getNodeAttribute(node, "type"); //$NON-NLS-1$
		if (typeName != null && typeName.equals("web")) { //$NON-NLS-1$
			fSiteType = WEB_SITE;
		}
	}

	@Override
	public void setURL(URL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fUrl;
		this.fUrl = url;
		firePropertyChanged(this, P_URL, oldValue, url);
	}

	@Override
	public void setSiteType(int type) throws CoreException {
		ensureModelEditable();
		Integer oldValue = Integer.valueOf(this.fSiteType);
		this.fSiteType = type;
		firePropertyChanged(this, P_URL, oldValue, Integer.valueOf(type));
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_URL)) {
			setURL((URL) newValue);
		} else if (name.equals(P_SITE_TYPE)) {
			setSiteType(((Integer) newValue).intValue());
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
	public String toString() {
		if (label != null) {
			return label;
		}
		if (fUrl != null) {
			return fUrl.toString();
		}
		return super.toString();
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		String tag = null;
		switch (fElementType) {
			case UPDATE :
				tag = "update"; //$NON-NLS-1$
				break;
			case DISCOVERY :
				tag = "discovery"; //$NON-NLS-1$
				break;
		}
		if (tag == null) {
			return;
		}
		writer.print(indent + "<" + tag); //$NON-NLS-1$
		if (label != null && label.length() > 0) {
			writer.print(" label=\"" + getWritableString(label) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fUrl != null) {
			writer.print(" url=\"" + getWritableString(fUrl.toString()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fSiteType == WEB_SITE) {
			writer.print(" type=\"web\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
	}
}
