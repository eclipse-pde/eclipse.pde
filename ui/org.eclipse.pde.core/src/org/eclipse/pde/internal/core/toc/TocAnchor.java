/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.toc;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.w3c.dom.Element;

/**
 * The TocAnchor class represents an anchor, which is used as a point
 * of inclusion for other tables of contents.
 * For instance, if TOC A contains the anchor with ID "PDE"
 * and TOC B has the "PDE" ID in its link_to attribute,
 * then the contents of TOC B will replace the anchor specified by
 * TOC A at runtime.
 * 
 * TOC anchors cannot have any content within them, so they are leaf objects.
 */
public class TocAnchor extends TocLeafObject {

	private static final long serialVersionUID = 1L;
	
	//TOC anchors only have one attribute:
	//the ID of the anchor
	private String fFieldAnchorId;
	
	/**
	 * Constructs an anchor with the given model and parent.
	 * 
	 * @param model The model associated with the new anchor.
	 * @param parent The parent TocObject of the new anchor.
	 */
	public TocAnchor(TocModel model, TocObject parent) {
		super(model, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_ANCHOR;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ANCHOR;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		parseFieldAnchorId(element);
	}

	/**
	 * Process the ID attribute for TOC anchor elements.
	 * 
	 * @param element The XML element to parse
	 */
	private void parseFieldAnchorId(Element element) {
		// Process label attribute
		// Trim leading and trailing whitespace
		fFieldAnchorId = element.getAttribute(ATTRIBUTE_ID).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#reset()
	 */
	public void reset() {
		fFieldAnchorId = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		if ((fFieldAnchorId != null) && 
				(fFieldAnchorId.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_ID, fFieldAnchorId));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getName()
	 */
	public String getName() {
		return fFieldAnchorId;
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
		return fFieldAnchorId;
	}

	/**
	 * Change the value of the anchor ID and 
	 * signal a model change if needed.
	 * 
	 * @param id The new ID to associate with the anchor
	 */
	public void setFieldAnchorId(String id) {
		String old = fFieldAnchorId;
		fFieldAnchorId = id;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_ID, old, fFieldAnchorId);
		}
	}
	
}
