/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class FeatureURLElement
	extends FeatureObject
	implements IFeatureURLElement {
	private int elementType;
	private int siteType = UPDATE_SITE;
	private URL url;

	public FeatureURLElement(int elementType) {
		this.elementType = elementType;
	}
	public FeatureURLElement(int elementType, URL url) {
		this.elementType = elementType;
		this.url = url;
	}
	public int getElementType() {
		return elementType;
	}
	public URL getURL() {
		return url;
	}
	public int getSiteType() {
		return siteType;
	}
	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		bindSourceLocation(node, lineTable);
		String urlName = getNodeAttribute(node, "url"); //$NON-NLS-1$
		try {
			url = new URL(urlName);
		} catch (MalformedURLException e) {
		}
		String typeName = getNodeAttribute(node, "type"); //$NON-NLS-1$
		if (typeName != null && typeName.equals("web")) //$NON-NLS-1$
			siteType = WEB_SITE;
	}

	public void setURL(URL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(this, P_URL, oldValue, url);
	}

	public void setSiteType(int type) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.siteType);
		this.siteType = type;
		firePropertyChanged(this, P_URL, oldValue, new Integer(type));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_URL)) {
			setURL((URL) newValue);
		} else if (name.equals(P_SITE_TYPE)) {
			setSiteType(((Integer) newValue).intValue());
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public String toString() {
		if (label != null)
			return label;
		if (url != null)
			return url.toString();
		return super.toString();
	}
	public void write(String indent, PrintWriter writer) {
		String tag = null;
		switch (elementType) {
			case UPDATE :
				tag = "update"; //$NON-NLS-1$
				break;
			case DISCOVERY :
				tag = "discovery"; //$NON-NLS-1$
				break;
		}
		if (tag == null)
			return;
		writer.print(indent + "<" + tag); //$NON-NLS-1$
		if (label != null) {
			writer.print(" label=\"" + getWritableString(label) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (url != null) {
			writer.print(" url=\"" + getWritableString(url.toString()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (siteType == WEB_SITE) {
			writer.print(" type=\"web\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
	}
}
