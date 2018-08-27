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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
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

	@Override
	public List<ICompCSTaskObject> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getElement() {
		return ELEMENT_PARAM;
	}

	@Override
	public String getName() {
		return fFieldName;
	}

	@Override
	public int getType() {
		return TYPE_PARAM;
	}

	@Override
	protected void parseAttributes(Element element) {
		// Process name attribute
		// Trim leading and trailing whitespace
		fFieldName = element.getAttribute(ATTRIBUTE_NAME).trim();
		// Process value attribute
		// Trim leading and trailing whitespace
		fFieldValue = element.getAttribute(ATTRIBUTE_VALUE).trim();
	}

	@Override
	protected void parseElement(Element element) {
		// NO-OP
	}

	@Override
	public void reset() {
		fFieldName = null;
		fFieldValue = null;
	}

	@Override
	protected void writeAttributes(StringBuilder buffer) {
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

	@Override
	protected void writeElements(String indent, PrintWriter writer) {
		// NO-OP
	}

	@Override
	public String getFieldName() {
		return fFieldName;
	}

	@Override
	public String getFieldValue() {
		return fFieldValue;
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
	public void setFieldValue(String value) {
		String old = fFieldValue;
		fFieldValue = value;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_VALUE, old, fFieldValue);
		}
	}

	@Override
	protected void parseText(Text text) {
		// NO-OP
	}

}
