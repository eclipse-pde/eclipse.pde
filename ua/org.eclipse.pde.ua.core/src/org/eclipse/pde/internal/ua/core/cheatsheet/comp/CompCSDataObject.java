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
import org.eclipse.pde.internal.ua.core.CheatSheetUtil;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDataObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public abstract class CompCSDataObject extends CompCSObject implements
		ICompCSDataObject {

	private static final long serialVersionUID = 1L;
	private String fFieldContent;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSDataObject(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		// Reset called by child class
	}

	@Override
	public List<ICompCSTaskObject> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public abstract String getElement();

	@Override
	public String getName() {
		return fFieldContent;
	}

	@Override
	public abstract int getType();

	@Override
	protected void parseContent(Element element) {
		// Override to handle unusual mixed content as in this case
		// Trim leading and trailing whitespace
		fFieldContent = CheatSheetUtil.parseElementText(element).trim();
	}

	@Override
	protected void parseAttributes(Element element) {
		// NO-OP
	}

	@Override
	protected void parseElement(Element element) {
		// NO-OP
	}

	@Override
	protected void parseText(Text text) {
		// NO-OP
	}

	@Override
	public void reset() {
		fFieldContent = null;
	}

	@Override
	protected void writeAttributes(StringBuilder buffer) {
		// NO-OP
	}

	@Override
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print contents
		if ((fFieldContent != null) && (fFieldContent.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			// Preserve tag exceptions
			writer.write(newIndent
					+ PDETextHelper.translateWriteText(fFieldContent.trim(),
							DEFAULT_TAG_EXCEPTIONS, DEFAULT_SUBSTITUTE_CHARS)
					+ "\n"); //$NON-NLS-1$
		}
	}

	@Override
	public String getFieldContent() {
		return fFieldContent;
	}

	@Override
	public void setFieldContent(String content) {
		String old = fFieldContent;
		fFieldContent = content;
		if (isEditable()) {
			firePropertyChanged(getElement(), old, fFieldContent);
		}
	}

}
