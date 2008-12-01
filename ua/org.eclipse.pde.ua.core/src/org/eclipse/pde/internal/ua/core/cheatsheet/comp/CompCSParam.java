/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class CompCSParam extends CompCSObject implements ICompCSParam {

	private String fFieldName;

	private String fFieldValue;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSParam(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_PARAM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getName()
	 */
	public String getName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getType()
	 */
	public int getType() {
		return TYPE_PARAM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseAttributes
	 * (org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		// Process name attribute
		// Trim leading and trailing whitespace
		fFieldName = element.getAttribute(ATTRIBUTE_NAME).trim();
		// Process value attribute
		// Trim leading and trailing whitespace
		fFieldValue = element.getAttribute(ATTRIBUTE_VALUE).trim();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseElement
	 * (org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		// NO-OP
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#reset()
	 */
	public void reset() {
		fFieldName = null;
		fFieldValue = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#writeAttributes
	 * (java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		// Print name attribute
		if ((fFieldName != null) && (fFieldName.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_NAME,
					fFieldName));
		}
		// Print value attribute
		if ((fFieldValue != null) && (fFieldValue.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_VALUE,
					PDETextHelper.translateWriteText(fFieldValue.trim(),
							DEFAULT_SUBSTITUTE_CHARS)));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#writeElements
	 * (java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		// NO-OP
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam#getFieldName
	 * ()
	 */
	public String getFieldName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam#getFieldValue
	 * ()
	 */
	public String getFieldValue() {
		return fFieldValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam#setFieldName
	 * (java.lang.String)
	 */
	public void setFieldName(String name) {
		String old = fFieldName;
		fFieldName = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_NAME, old, fFieldName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam#setFieldValue
	 * (java.lang.String)
	 */
	public void setFieldValue(String value) {
		String old = fFieldValue;
		fFieldValue = value;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_VALUE, old, fFieldValue);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseText(
	 * org.w3c.dom.Text)
	 */
	protected void parseText(Text text) {
		// NO-OP
	}

}
