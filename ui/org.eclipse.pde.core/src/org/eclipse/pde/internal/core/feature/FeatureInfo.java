/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.w3c.dom.Node;

public class FeatureInfo extends FeatureObject implements IFeatureInfo {
	private static final long serialVersionUID = 1L;
	private String url;
	private String description;
	private final int index;

	public FeatureInfo(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	private String getTag() {
		return IFeature.INFO_TAGS[index];
	}

	@Override
	public String getURL() {
		return url;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		switch (name) {
		case P_DESC:
			setDescription(newValue != null ? newValue.toString() : null);
			break;
		case P_URL:
			setURL(newValue != null ? newValue.toString() : null);
			break;
		default:
			super.restoreProperty(name, oldValue, newValue);
			break;
		}
	}

	@Override
	public void setDescription(String description) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESC, oldValue, description);
	}

	@Override
	protected void parse(Node node) {
		url = getNodeAttribute(node, "url"); //$NON-NLS-1$
		Node firstChild = node.getFirstChild();
		if (firstChild != null) {
			description = getNormalizedText(firstChild.getNodeValue());
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		String indent2 = indent + Feature.INDENT;
		String desc = description != null ? getWritableString(description.trim()) : null;
		writer.println();
		writer.print(indent + "<" + getTag()); //$NON-NLS-1$
		if (url != null) {
			writer.print(" url=\"" + getWritableString(url) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		if (desc != null) {
			writer.println(indent2 + desc);
		}
		writer.println(indent + "</" + getTag() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean isEmpty() {
		if (url != null) {
			return false;
		}
		String desc = description != null ? description.trim() : null;
		if (desc != null && desc.length() > 0) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		switch (index) {
			case IFeature.INFO_DESCRIPTION :
				return PDECoreMessages.FeatureInfo_description;
			case IFeature.INFO_LICENSE :
				return PDECoreMessages.FeatureInfo_license;
			case IFeature.INFO_COPYRIGHT :
				return PDECoreMessages.FeatureInfo_copyright;
		}
		return super.toString();
	}
}
