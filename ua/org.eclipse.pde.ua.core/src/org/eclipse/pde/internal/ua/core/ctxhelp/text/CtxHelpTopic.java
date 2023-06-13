/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.ctxhelp.text;

import org.eclipse.core.runtime.IPath;

/**
 * Represents a topic entry in context help. Topics are used to open related
 * help in other files. Topics are leaf objects.
 *
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public class CtxHelpTopic extends CtxHelpObject {

	private static final long serialVersionUID = 1L;

	public CtxHelpTopic(CtxHelpModel model) {
		super(model, ELEMENT_TOPIC);
	}

	@Override
	public boolean canBeParent() {
		return false;
	}

	@Override
	public int getType() {
		return TYPE_TOPIC;
	}

	@Override
	public String getName() {
		return getLabel();
	}

	/**
	 * @return the value of the label attribute or <code>null</code> if the
	 *         attribute does not exist
	 */
	public String getLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	/**
	 * Set the label attribute, passing <code>null</code> will set the attribute
	 * to be empty.
	 *
	 * @param label
	 *            new value
	 */
	public void setLabel(String label) {
		setXMLAttribute(ATTRIBUTE_LABEL, label);
	}

	/**
	 * @return the value of the href attribute as a path or <code>null</code>
	 */
	public IPath getLocation() {
		String value = getXMLAttributeValue(ATTRIBUTE_HREF);
		if (value != null) {
			return IPath.fromOSString(value);
		}
		return null;
	}

	/**
	 * Set the href (link) attribute, passing <code>null</code> will set the
	 * attribute to be empty.
	 *
	 * @param path
	 *            new value
	 */
	public void setLocation(IPath path) {
		if (path == null) {
			setXMLAttribute(ATTRIBUTE_HREF, null);
		} else {
			setXMLAttribute(ATTRIBUTE_HREF, path.toPortableString());
		}
	}

	@Override
	public boolean canAddChild(int objectType) {
		return false;
	}

	@Override
	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_COMMAND || objectType == TYPE_TOPIC;
	}
}
