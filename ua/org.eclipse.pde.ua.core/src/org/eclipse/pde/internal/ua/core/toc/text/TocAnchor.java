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
 * The TocAnchor class represents an anchor, which is used as a point of
 * inclusion for other tables of contents. For instance, if TOC A contains the
 * anchor with ID "PDE" and TOC B has the "PDE" ID in its link_to attribute,
 * then the contents of TOC B will replace the anchor specified by TOC A at
 * runtime.
 * 
 * TOC anchors cannot have any content within them, so they are leaf objects.
 */
public class TocAnchor extends TocObject {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an anchor with the given model and parent.
	 * 
	 * @param parent
	 *            The parent TocObject of the new anchor.
	 */
	public TocAnchor(TocModel model) {
		super(model, ELEMENT_ANCHOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ANCHOR;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.toc.TocObject#getName()
	 */
	public String getName() {
		return getFieldAnchorId();
	}

	public String getPath() {
		// Since the anchor is never associated with any file,
		// the path is null.
		return null;
	}

	/**
	 * @return the ID of this anchor
	 */
	public String getFieldAnchorId() {
		return getXMLAttributeValue(ATTRIBUTE_ID);
	}

	/**
	 * Change the value of the anchor ID and signal a model change if needed.
	 * 
	 * @param id
	 *            The new ID to associate with the anchor
	 */
	public void setFieldAnchorId(String id) {
		setXMLAttribute(ATTRIBUTE_ID, id);
	}
}
