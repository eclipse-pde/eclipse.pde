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

import java.util.ArrayList;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;

public class SimpleCS extends SimpleCSObject implements ISimpleCS {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCS(ISimpleCSModel model) {
		super(model, ELEMENT_CHEATSHEET);
		// Root node
		setInTheModel(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#addItem(org
	 * .eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(ISimpleCSItem item) {
		addChildNode((IDocumentElementNode) item, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#addItem(int,
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(int index, ISimpleCSItem item) {
		addChildNode((IDocumentElementNode) item, index, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getIntro()
	 */
	public ISimpleCSIntro getIntro() {
		return (ISimpleCSIntro) getChildNode(ISimpleCSIntro.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getItemCount()
	 */
	public int getItemCount() {
		return getChildNodeCount(ISimpleCSItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getItems()
	 */
	public ISimpleCSItem[] getItems() {
		ArrayList filteredChildren = getChildNodesList(ISimpleCSItem.class,
				true);
		return (ISimpleCSItem[]) filteredChildren
				.toArray(new ISimpleCSItem[filteredChildren.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getNextSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getNextSibling(ISimpleCSItem item) {
		return (ISimpleCSItem) getNextSibling((IDocumentElementNode) item,
				ISimpleCSItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getPreviousSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item) {
		return (ISimpleCSItem) getPreviousSibling((IDocumentElementNode) item,
				ISimpleCSItem.class);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#getTitle()
	 */
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#hasItems()
	 */
	public boolean hasItems() {
		return hasChildNodes(ISimpleCSItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#indexOfItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public int indexOfItem(ISimpleCSItem item) {
		return indexOf((IDocumentElementNode) item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#isFirstItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isFirstItem(ISimpleCSItem item) {
		return isFirstChildNode((IDocumentElementNode) item,
				ISimpleCSItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#isLastItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isLastItem(ISimpleCSItem item) {
		return isLastChildNode((IDocumentElementNode) item, ISimpleCSItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#moveItem(org
	 * .eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem, int)
	 */
	public void moveItem(ISimpleCSItem item, int newRelativeIndex) {
		moveChildNode((IDocumentElementNode) item, newRelativeIndex, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#removeItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void removeItem(ISimpleCSItem item) {
		removeChildNode((IDocumentElementNode) item, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#removeItem
	 * (int)
	 */
	public void removeItem(int index) {
		removeChildNode(index, ISimpleCSItem.class, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#setIntro(org
	 * .eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSIntro)
	 */
	public void setIntro(ISimpleCSIntro intro) {
		setChildNode((IDocumentElementNode) intro, ISimpleCSIntro.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCS#setTitle(java
	 * .lang.String)
	 */
	public void setTitle(String title) {
		setXMLAttribute(ATTRIBUTE_TITLE, title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getName
	 * ()
	 */
	public String getName() {
		return getTitle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_CHEAT_SHEET;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

}
