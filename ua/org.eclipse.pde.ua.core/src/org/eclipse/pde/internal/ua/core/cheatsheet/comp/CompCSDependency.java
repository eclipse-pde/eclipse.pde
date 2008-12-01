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
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class CompCSDependency extends CompCSObject implements ICompCSDependency {

	private String fFieldTask;

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSDependency(ICompCSModel model, ICompCSObject parent) {
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
		return ELEMENT_DEPENDENCY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getName()
	 */
	public String getName() {
		return fFieldTask;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getType()
	 */
	public int getType() {
		return TYPE_DEPENDENCY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseAttributes
	 * (org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		// Process task attribute
		// Trim leading and trailing whitespace
		fFieldTask = element.getAttribute(ATTRIBUTE_TASK).trim();
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
		fFieldTask = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#writeAttributes
	 * (java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		// Print task attribute
		if ((fFieldTask != null) && (fFieldTask.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_TASK,
					PDETextHelper.translateWriteText(fFieldTask.trim(),
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
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency#getFieldTask
	 * ()
	 */
	public String getFieldTask() {
		return fFieldTask;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency#setFieldTask
	 * (java.lang.String)
	 */
	public void setFieldTask(String task) {
		String old = fFieldTask;
		fFieldTask = task;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_TASK, old, fFieldTask);
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
