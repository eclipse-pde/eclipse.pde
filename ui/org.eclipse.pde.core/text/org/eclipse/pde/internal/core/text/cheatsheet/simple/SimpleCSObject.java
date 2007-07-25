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

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.plugin.IWritableDelimiter;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;
import org.w3c.dom.Element;

/**
 * SimpleCSObject
 *
 */
public abstract class SimpleCSObject extends DocumentObject implements
		ISimpleCSObject, IWritableDelimiter {

	
	// TODO: MP: TEO:  Make ISimpleCSObject extend IDocumentObject
	
	// TODO: MP: TEO:  Compare with CompCS model to add in handy utility methods for style
	
	private static final long serialVersionUID = 1L;

	protected static final HashSet TAG_EXCEPTIONS = new HashSet(3);
	
	// TODO: MP: TEO:  May not need this - delete
	protected static final HashMap SUBSTITUTE_CHARS = new HashMap(5);

	
	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
		
		SUBSTITUTE_CHARS.put(new Character('&'), "&amp;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('<'), "&lt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('>'), "&gt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\''), "&apos;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\"'), "&quot;"); //$NON-NLS-1$
	}	
	
	/**
	 * @param model
	 * @param tagName
	 */
	public SimpleCSObject(ISimpleCSModel model, String tagName) {
		super(model, tagName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return getChildNodesList(DocumentGenericNode.class, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getModel()
	 */
	public ISimpleCSModel getModel() {
		return (ISimpleCSModel)getSharedModel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getName()
	 */
	public abstract String getName();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getParent()
	 */
	public ISimpleCSObject getParent() {
		return (ISimpleCSObject)getParentNode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getSimpleCS()
	 */
	public ISimpleCS getSimpleCS() {
		return getModel().getSimpleCS();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public abstract int getType();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// TODO: MP: TEO: Candidate for removal and refactor
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#setModel(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel)
	 */
	public void setModel(ISimpleCSModel model) {
		setSharedModel(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// TODO: MP: TEO: Probably \n for all
		// NO-OP
		// Child classes to override
	}

}
