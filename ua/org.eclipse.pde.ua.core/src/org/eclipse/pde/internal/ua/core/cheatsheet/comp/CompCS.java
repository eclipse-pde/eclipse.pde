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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * CompCS
 * 
 */
public class CompCS extends CompCSObject implements ICompCS {

	private String fFieldName;

	private ICompCSTaskObject fFieldTaskObject;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public CompCS(ICompCSModel model) {
		super(model, null);
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add task / taskGroup
		if (fFieldTaskObject != null) {
			list.add(fFieldTaskObject);
		}
		return list;
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
		return TYPE_COMPOSITE_CHEATSHEET;
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseElement
	 * (org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		String name = element.getNodeName();
		ICompCSModelFactory factory = getModel().getFactory();

		if (name.equals(ELEMENT_TASK)) {
			// Process task element
			fFieldTaskObject = factory.createCompCSTask(this);
			fFieldTaskObject.parse(element);
		} else if (name.equals(ELEMENT_TASKGROUP)) {
			// Process taskGroup element
			fFieldTaskObject = factory.createCompCSTaskGroup(this);
			fFieldTaskObject.parse(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#reset()
	 */
	public void reset() {
		fFieldName = null;
		fFieldTaskObject = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#write(java
	 * .lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		try {
			// Print XML decl
			XMLPrintHandler.printHead(writer, ATTRIBUTE_VALUE_ENCODING);
			super.write(indent, writer);
		} catch (IOException e) {
			// Suppress
			// e.printStackTrace();
		}
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
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_NAME,
					PDETextHelper.translateWriteText(fFieldName.trim(),
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
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print task / taskGroup element
		if (fFieldTaskObject != null) {
			fFieldTaskObject.write(newIndent, writer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS#getFieldName()
	 */
	public String getFieldName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS#getFieldTaskObject
	 * ()
	 */
	public ICompCSTaskObject getFieldTaskObject() {
		return fFieldTaskObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS#setFieldName(java
	 * .lang.String)
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
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS#setFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public void setFieldTaskObject(ICompCSTaskObject taskObject) {
		ICompCSObject old = fFieldTaskObject;
		fFieldTaskObject = taskObject;
		if (isEditable()) {
			fireStructureChanged(fFieldTaskObject, old);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_COMPOSITE_CHEATSHEET;
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
