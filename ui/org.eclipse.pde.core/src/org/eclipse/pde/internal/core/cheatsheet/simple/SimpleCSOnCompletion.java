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

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;

/**
 * SimpleCSOnCompletion
 *
 */
public class SimpleCSOnCompletion extends SimpleCSObject implements
		ISimpleCSOnCompletion {

	/**
	 * Content (Element)
	 */
	private String fContent;	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSOnCompletion(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Element element) {
		fContent = parseElementText(element).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		
		try {
			// Start element
			XMLPrintHandler.printBeginElement(writer, ELEMENT_ONCOMPLETION,
					indent, false);
			// Print contents
			if ((fContent != null) &&
					(fContent.length() > 0)) {
				writer.write(newIndent
						+ PDETextHelper.translateWriteText(fContent.trim(),
								TAG_EXCEPTIONS, SUBSTITUTE_CHARS) + "\n");				 //$NON-NLS-1$
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_ONCOMPLETION, indent);
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fContent = null;
	}

	/**
	 * Content (element)
	 * @return
	 */
	public String getContent() {
		return fContent;
	}

	/**
	 * Content (element)
	 * @param content
	 */
	public void setContent(String content) {
		String old = fContent;
		fContent = content;
		if (isEditable()) {
			firePropertyChanged(ELEMENT_DESCRIPTION, old, fContent);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_ON_COMPLETION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// TODO: MP: Update name
		return ELEMENT_ONCOMPLETION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

}
