/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;
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

	@Override
	public List<ICompCSTaskObject> getChildren() {
		List<ICompCSTaskObject> list = new ArrayList<>();
		// Add task / taskGroup
		if (fFieldTaskObject != null) {
			list.add(fFieldTaskObject);
		}
		return list;
	}

	@Override
	public String getName() {
		return fFieldName;
	}

	@Override
	public int getType() {
		return TYPE_COMPOSITE_CHEATSHEET;
	}

	@Override
	protected void parseAttributes(Element element) {
		// Process name attribute
		// Trim leading and trailing whitespace
		fFieldName = element.getAttribute(ATTRIBUTE_NAME).trim();
	}

	@Override
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

	@Override
	public void reset() {
		fFieldName = null;
		fFieldTaskObject = null;
	}

	@Override
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

	@Override
	protected void writeAttributes(StringBuilder buffer) {
		// Print name attribute
		if ((fFieldName != null) && (fFieldName.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_NAME,
					PDETextHelper.translateWriteText(fFieldName.trim(),
							DEFAULT_SUBSTITUTE_CHARS)));
		}
	}

	@Override
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print task / taskGroup element
		if (fFieldTaskObject != null) {
			fFieldTaskObject.write(newIndent, writer);
		}
	}

	@Override
	public String getFieldName() {
		return fFieldName;
	}

	@Override
	public ICompCSTaskObject getFieldTaskObject() {
		return fFieldTaskObject;
	}

	@Override
	public void setFieldName(String name) {
		String old = fFieldName;
		fFieldName = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_NAME, old, fFieldName);
		}
	}

	@Override
	public void setFieldTaskObject(ICompCSTaskObject taskObject) {
		ICompCSObject old = fFieldTaskObject;
		fFieldTaskObject = taskObject;
		if (isEditable()) {
			fireStructureChanged(fFieldTaskObject, old);
		}
	}

	@Override
	public String getElement() {
		return ELEMENT_COMPOSITE_CHEATSHEET;
	}

	@Override
	protected void parseText(Text text) {
		// NO-OP
	}

}
