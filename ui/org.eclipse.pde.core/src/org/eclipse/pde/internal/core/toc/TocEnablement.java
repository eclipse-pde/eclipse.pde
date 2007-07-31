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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/**
 * The TocEnablement class represents enablement in a TOC element.
 * Enablement is a condition that the environment must fulfill in order
 * for the element in question to appear.
 */
public class TocEnablement extends TocLeafObject {

	protected String fEnablementSource;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a topic with the given model and parent.
	 * 
	 * @param model The model associated with the new topic.
	 * @param parent The parent TocObject of the new topic.
	 */
	public TocEnablement(TocModel model, TocObject parent) {
		super(model, parent);
	}

	/**
	 * @return a copy of the current list of children for this topic.
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}
	
	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getName() {
		return ELEMENT_ENABLEMENT;
	}
	
	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_ENABLEMENT;
	}

	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ENABLEMENT;
	}

	/**
	 * Parses the given element's attributes and children.
	 * 
	 * @param element The XML element to parse.
	 */
	public void parse(Element element) {
		fEnablementSource = element.toString();
	}
	
	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
	}

	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#reset()
	 */
	public void reset() {
		fEnablementSource = null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
	}

	/**
	 * @return the link associated with this topic, <br />
	 * or <code>null</code> if none exists.
	 */
	public String getEnablementSource() {
		return fEnablementSource;
	}

	/**
	 * Change the value of the link field and 
	 * signal a model change if needed.
	 */
	public void setFieldRef(String value) {
		String old = fEnablementSource;
		fEnablementSource = value;
		if (isEditable()) {
			firePropertyChanged(ELEMENT_ENABLEMENT, old, fEnablementSource);
		}
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}
}
