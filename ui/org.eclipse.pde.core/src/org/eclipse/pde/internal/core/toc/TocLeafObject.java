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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * The TocLeafObject class is an abstract class specifically used
 * for TOC objects that cannot have children. Subclasses do not have
 * to provide implementation for child-based functionality (i.e. getChildren())
 */
public abstract class TocLeafObject extends TocObject {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param model The model associated with the new object.
	 * @param parent The parent TocObject of the new object.
	 */
	public TocLeafObject(TocModel model, TocObject parent) {
		super(model, parent);
	}
	
	/**
	 * @return an empty list, since leaves do not have children
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseElement(org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		// NO-OP: Leaf elements do not have children
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		super.write(indent, writer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeElements(java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		// NO-OP: Leaf objects do not have children		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseText(org.w3c.dom.Text)
	 */
	protected void parseText(Text text) {
		// NO-OP: Leaf objects do not have text contents
	}

}
