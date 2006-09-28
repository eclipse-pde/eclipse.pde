/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSDataObject;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.core.util.CheatSheetUtil;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;

/**
 * CompCSDataObject
 *
 */
public abstract class CompCSDataObject extends CompCSObject implements
		ICompCSDataObject {

	private String fFieldContent;
	
	/**
	 * @param model
	 * @param parent
	 */
	public CompCSDataObject(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		// Reset called by child class
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#getElement()
	 */
	public abstract String getElement();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#getName()
	 */
	public String getName() {
		return fFieldContent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#getType()
	 */
	public abstract int getType();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#parseElement(org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		// Trim leading and trailing whitespace		
		fFieldContent = CheatSheetUtil.parseElementText(element).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#reset()
	 */
	public void reset() {
		fFieldContent = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSObject#writeElements(java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;		
		// Print contents
		if ((fFieldContent != null) &&
				(fFieldContent.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			// Preserve tag exceptions
			writer.write(newIndent
					+ PDETextHelper.translateWriteText(fFieldContent.trim(),
							TAG_EXCEPTIONS, SUBSTITUTE_CHARS) + "\n"); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSDataObject#getFieldContent()
	 */
	public String getFieldContent() {
		return fFieldContent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSDataObject#setFieldContent(java.lang.String)
	 */
	public void setFieldContent(String content) {
		String old = fFieldContent;
		fFieldContent = content;
		if (isEditable()) {
			firePropertyChanged(getElement(), old, fFieldContent);
		}	
	}

}
