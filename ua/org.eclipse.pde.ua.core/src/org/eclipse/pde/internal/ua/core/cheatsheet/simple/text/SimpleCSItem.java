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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;

public class SimpleCSItem extends SimpleCSObject implements ISimpleCSItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSItem(ISimpleCSModel model) {
		super(model, ELEMENT_ITEM);
	}

	@Override
	public void addSubItem(ISimpleCSSubItemObject subitem) {
		addChildNode(subitem, true);
	}

	@Override
	public void addSubItem(int index, ISimpleCSSubItemObject subitem) {
		addChildNode(subitem, index, true);
	}

	@Override
	public ISimpleCSDescription getDescription() {
		return (ISimpleCSDescription) getChildNode(ISimpleCSDescription.class);
	}

	@Override
	public boolean getDialog() {
		return getBooleanAttributeValue(ATTRIBUTE_DIALOG, false);
	}

	@Override
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem) {
		return (ISimpleCSSubItemObject) getNextSibling(
				subitem, ISimpleCSSubItemObject.class);
	}

	@Override
	public ISimpleCSOnCompletion getOnCompletion() {
		return (ISimpleCSOnCompletion) getChildNode(ISimpleCSOnCompletion.class);
	}

	@Override
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem) {
		return (ISimpleCSSubItemObject) getPreviousSibling(
				subitem, ISimpleCSSubItemObject.class);
	}

	@Override
	public boolean getSkip() {
		return getBooleanAttributeValue(ATTRIBUTE_SKIP, false);
	}

	@Override
	public int getSubItemCount() {
		return getChildNodeCount(ISimpleCSSubItemObject.class);
	}

	@Override
	public ISimpleCSSubItemObject[] getSubItems() {
		List<IDocumentElementNode> filteredChildren = getChildNodesList(ISimpleCSSubItemObject.class, true);
		return filteredChildren
				.toArray(new ISimpleCSSubItemObject[filteredChildren.size()]);
	}

	@Override
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	@Override
	public boolean hasSubItems() {
		return hasChildNodes(ISimpleCSSubItemObject.class);
	}

	@Override
	public int indexOfSubItem(ISimpleCSSubItemObject subitem) {
		return indexOf(subitem);
	}

	@Override
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem) {
		return isFirstChildNode(subitem,
				ISimpleCSSubItemObject.class);
	}

	@Override
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem) {
		return isLastChildNode(subitem,
				ISimpleCSSubItemObject.class);
	}

	@Override
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex) {
		moveChildNode(subitem, newRelativeIndex, true);
	}

	@Override
	public void removeSubItem(ISimpleCSSubItemObject subitem) {
		removeChildNode(subitem, true);
	}

	@Override
	public void removeSubItem(int index) {
		removeChildNode(index, ISimpleCSSubItemObject.class, true);
	}

	@Override
	public void setDescription(ISimpleCSDescription description) {
		setChildNode(description,
				ISimpleCSDescription.class);
	}

	@Override
	public void setDialog(boolean dialog) {
		setBooleanAttributeValue(ATTRIBUTE_DIALOG, dialog);
	}

	@Override
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion) {
		setChildNode(onCompletion,
				ISimpleCSOnCompletion.class);
	}

	@Override
	public void setSkip(boolean skip) {
		setBooleanAttributeValue(ATTRIBUTE_SKIP, skip);
	}

	@Override
	public void setTitle(String title) {
		setXMLAttribute(ATTRIBUTE_TITLE, title);
	}

	@Override
	public String getContextId() {
		return getXMLAttributeValue(ATTRIBUTE_CONTEXTID);
	}

	@Override
	public String getHref() {
		return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	@Override
	public void setContextId(String contextId) {
		setXMLAttribute(ATTRIBUTE_CONTEXTID, contextId);
	}

	@Override
	public void setHref(String href) {
		setXMLAttribute(ATTRIBUTE_HREF, href);
	}

	@Override
	public ISimpleCSRunContainerObject getExecutable() {
		return (ISimpleCSRunContainerObject) getChildNode(ISimpleCSRunContainerObject.class);
	}

	@Override
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		setChildNode(executable,
				ISimpleCSRunContainerObject.class);
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public int getType() {
		return TYPE_ITEM;
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		// Add subitems
		// Add unsupported perform-when if it is set as the executable
		Class<?>[] classes = { ISimpleCSSubItemObject.class,
				ISimpleCSPerformWhen.class };
		return getChildNodesList(classes, true);
	}

}
