/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class FeatureInfo extends FeatureObject implements IFeatureInfo {
	private static final long serialVersionUID = 1L;
	private static final String KEY_INFO_DESCRIPTION =
		"FeatureInfo.description"; //$NON-NLS-1$
	private static final String KEY_INFO_LICENSE = "FeatureInfo.license"; //$NON-NLS-1$
	private static final String KEY_INFO_COPYRIGHT =
		"FeatureInfo.copyright"; //$NON-NLS-1$
	private String url;
	private String description;
	private int index;

	public FeatureInfo(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	private String getTag() {
		return IFeature.INFO_TAGS[index];
	}

	/*
	 * @see IFeatureInfo#getURL()
	 */
	public String getURL() {
		return url;
	}

	/*
	 * @see IFeatureInfo#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/*
	 * @see IFeatureInfo#setURL(URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_DESC)) {
			setDescription(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	/*
	 * @see IFeatureInfo#setDescription(String)
	 */
	public void setDescription(String description) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESC, oldValue, description);
	}
	protected void parse(Node node, Hashtable lineTable) {
		bindSourceLocation(node, lineTable);
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
		Node firstChild = node.getFirstChild();
		if (firstChild!=null)
			description = getNormalizedText(firstChild.getNodeValue());
	}

	public void write(String indent, PrintWriter writer) {
		String indent2 = indent + Feature.INDENT;
		String desc = description!=null?getWritableString(description.trim()):null;
		writer.println();
		writer.print(indent + "<" + getTag()); //$NON-NLS-1$
		if (url != null) {
			writer.print(" url=\"" + url + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		if (desc!=null) writer.println(indent2 + desc);
		writer.println(indent + "</" + getTag() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isEmpty() {
		if (url != null)
			return false;
		String desc = description != null ? description.trim() : null;
		if (desc != null && desc.length() > 0)
			return false;
		return true;
	}

	public String toString() {
		switch (index) {
			case IFeature.INFO_DESCRIPTION :
				return PDECore.getResourceString(KEY_INFO_DESCRIPTION);
			case IFeature.INFO_LICENSE :
				return PDECore.getResourceString(KEY_INFO_LICENSE);
			case IFeature.INFO_COPYRIGHT :
				return PDECore.getResourceString(KEY_INFO_COPYRIGHT);
		}
		return super.toString();
	}
}
