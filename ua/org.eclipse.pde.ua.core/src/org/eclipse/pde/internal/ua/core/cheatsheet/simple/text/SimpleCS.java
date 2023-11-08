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

import java.util.List;

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

	@Override
	public void addItem(ISimpleCSItem item) {
		addChildNode(item, true);
	}

	@Override
	public void addItem(int index, ISimpleCSItem item) {
		addChildNode(item, index, true);
	}

	@Override
	public ISimpleCSIntro getIntro() {
		return (ISimpleCSIntro) getChildNode(ISimpleCSIntro.class);
	}

	@Override
	public int getItemCount() {
		return getChildNodeCount(ISimpleCSItem.class);
	}

	@Override
	public ISimpleCSItem[] getItems() {
		List<IDocumentElementNode> filteredChildren = getChildNodesList(ISimpleCSItem.class, true);
		return filteredChildren
				.toArray(new ISimpleCSItem[filteredChildren.size()]);
	}

	@Override
	public ISimpleCSItem getNextSibling(ISimpleCSItem item) {
		return (ISimpleCSItem) getNextSibling(item,
				ISimpleCSItem.class);
	}

	@Override
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item) {
		return (ISimpleCSItem) getPreviousSibling(item,
				ISimpleCSItem.class);

	}

	@Override
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	@Override
	public boolean hasItems() {
		return hasChildNodes(ISimpleCSItem.class);
	}

	@Override
	public int indexOfItem(ISimpleCSItem item) {
		return indexOf(item);
	}

	@Override
	public boolean isFirstItem(ISimpleCSItem item) {
		return isFirstChildNode(item,
				ISimpleCSItem.class);
	}

	@Override
	public boolean isLastItem(ISimpleCSItem item) {
		return isLastChildNode(item, ISimpleCSItem.class);
	}

	@Override
	public void moveItem(ISimpleCSItem item, int newRelativeIndex) {
		moveChildNode(item, newRelativeIndex, true);
	}

	@Override
	public void removeItem(ISimpleCSItem item) {
		removeChildNode(item, true);
	}

	@Override
	public void removeItem(int index) {
		removeChildNode(index, ISimpleCSItem.class, true);
	}

	@Override
	public void setIntro(ISimpleCSIntro intro) {
		setChildNode(intro, ISimpleCSIntro.class);
	}

	@Override
	public void setTitle(String title) {
		setXMLAttribute(ATTRIBUTE_TITLE, title);
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public int getType() {
		return TYPE_CHEAT_SHEET;
	}

	@Override
	public boolean isRoot() {
		return true;
	}

}
