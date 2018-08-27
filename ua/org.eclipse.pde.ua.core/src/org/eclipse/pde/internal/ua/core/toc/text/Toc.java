/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.core.toc.text;

/**
 * Toc - represents the root element of a Table of Contents The TOC root
 * element, like TOC topics, can hold many child topics, links and anchors.
 * Aside from being the root element of the TOC, the element differs from
 * regular topics by having an optional anchor attribute that determines which
 * anchors this TOC will plug into.
 */
public class Toc extends TocTopic {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new Toc. Only takes a model, since the root element cannot
	 * have a parent.
	 *
	 * @param model
	 *            The model associated with this TOC.
	 */
	public Toc(TocModel model) {
		super(model, ELEMENT_TOC);
		setInTheModel(true);
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public int getType() {
		return TYPE_TOC;
	}

	/**
	 * @return the link associated with this topic, <br />
	 *         or <code>null</code> if none exists.
	 */
	@Override
	public String getFieldRef() {
		return getXMLAttributeValue(ATTRIBUTE_TOPIC);
	}

	/**
	 * Change the value of the link field and signal a model change if needed.
	 *
	 * @param value
	 *            The new page location to be linked by this topic
	 */
	@Override
	public void setFieldRef(String value) {
		setXMLAttribute(ATTRIBUTE_TOPIC, value);
	}

	/**
	 * @return the anchor path associated with this TOC
	 */
	public String getFieldAnchorTo() {
		return getXMLAttributeValue(ATTRIBUTE_LINK_TO);
	}

	/**
	 * Change the value of the anchor field and signal a model change if needed.
	 */
	public void setFieldAnchorTo(String name) {
		setXMLAttribute(ATTRIBUTE_LINK_TO, name);
	}
}
