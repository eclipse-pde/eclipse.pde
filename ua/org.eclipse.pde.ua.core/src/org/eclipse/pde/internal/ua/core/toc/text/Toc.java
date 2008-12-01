/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_TOC;
	}

	/**
	 * @return the link associated with this topic, <br />
	 *         or <code>null</code> if none exists.
	 */
	public String getFieldRef() {
		return getXMLAttributeValue(ATTRIBUTE_TOPIC);
	}

	/**
	 * Change the value of the link field and signal a model change if needed.
	 * 
	 * @param value
	 *            The new page location to be linked by this topic
	 */
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
	 * 
	 * @param The
	 *            new anchor path to associate with this TOC
	 */
	public void setFieldAnchorTo(String name) {
		setXMLAttribute(ATTRIBUTE_LINK_TO, name);
	}
}
