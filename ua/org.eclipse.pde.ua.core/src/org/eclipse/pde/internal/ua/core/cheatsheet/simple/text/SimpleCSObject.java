/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.pde.internal.core.plugin.IWritableDelimiter;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
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

	@Override
	public List<IDocumentElementNode> getChildren() {
		return getChildNodesList(DocumentGenericNode.class, false);
	}

	@Override
	public ISimpleCSModel getModel() {
		return (ISimpleCSModel) getSharedModel();
	}

	@Override
	public abstract String getName();

	@Override
	public ISimpleCSObject getParent() {
		return (ISimpleCSObject) getParentNode();
	}

	@Override
	public ISimpleCS getSimpleCS() {
		return getModel().getSimpleCS();
	}

	@Override
	public abstract int getType();

	@Override
	public void parse(Element element) {
		// TODO: MP: TEO: LOW: Remove parse from interface - once old simpleCS
		// model is deleted
		// NO-OP
	}

	@Override
	public void setModel(ISimpleCSModel model) {
		setSharedModel(model);
	}

	@Override
	public void writeDelimeter(PrintWriter writer) {
		// TODO: MP: TEO: LOW: Probably \n for all
		// NO-OP
		// Child classes to override
	}

	@Override
	protected IDocumentTextNode createDocumentTextNode() {
		return new SimpleCSDocumentTextNode();
	}

}
