/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.pde.internal.core.plugin.IWritableDelimiter;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.w3c.dom.Element;

public abstract class SimpleCSObject extends DocumentObject implements
		ISimpleCSObject, IWritableDelimiter {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param tagName
	 */
	public SimpleCSObject(ISimpleCSModel model, String tagName) {
		super(model, tagName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		return getChildNodesList(DocumentGenericNode.class, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#getModel
	 * ()
	 */
	public ISimpleCSModel getModel() {
		return (ISimpleCSModel) getSharedModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#getName
	 * ()
	 */
	public abstract String getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#getParent
	 * ()
	 */
	public ISimpleCSObject getParent() {
		return (ISimpleCSObject) getParentNode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#getSimpleCS
	 * ()
	 */
	public ISimpleCS getSimpleCS() {
		return getModel().getSimpleCS();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#getType
	 * ()
	 */
	public abstract int getType();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#parse
	 * (org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// TODO: MP: TEO: LOW: Remove parse from interface - once old simpleCS
		// model is deleted
		// NO-OP
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSObject#setModel
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSModel)
	 */
	public void setModel(ISimpleCSModel model) {
		setSharedModel(model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter
	 * (java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// TODO: MP: TEO: LOW: Probably \n for all
		// NO-OP
		// Child classes to override
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#
	 * createDocumentTextNode()
	 */
	protected IDocumentTextNode createDocumentTextNode() {
		return new SimpleCSDocumentTextNode();
	}

}
