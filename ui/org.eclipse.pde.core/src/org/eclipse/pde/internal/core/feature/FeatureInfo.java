/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.w3c.dom.Node;

/**
 * @version 	1.0
 * @author
 */
public class FeatureInfo extends FeatureObject implements IFeatureInfo {
	private static final String KEY_INFO_DESCRIPTION =
		"FeatureEditor.info.description";
	private static final String KEY_INFO_LICENSE = "FeatureEditor.info.license";
	private static final String KEY_INFO_COPYRIGHT =
		"FeatureEditor.info.copyright";
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
	protected void parse(Node node) {
		url = getNodeAttribute(node, "url");
		description = getNormalizedText(node.getFirstChild().getNodeValue());
	}

	public void write(String indent, PrintWriter writer) {
		String indent2 = indent + Feature.INDENT;
		String desc = getWritableString(description.trim());
		writer.println();
		writer.print(indent + "<" + getTag());
		if (url != null) {
			writer.print(" url=\"" + url + "\"");
		}
		writer.println(">");
		writer.println(indent2 + desc);
		writer.println(indent + "</" + getTag() + ">");
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