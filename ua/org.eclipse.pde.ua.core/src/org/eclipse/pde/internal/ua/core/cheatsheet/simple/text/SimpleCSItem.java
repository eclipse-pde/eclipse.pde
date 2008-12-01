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
import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItemObject;

public class SimpleCSItem extends SimpleCSObject implements ISimpleCSItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSItem(ISimpleCSModel model) {
		super(model, ELEMENT_ITEM);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#addSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem) {
		addChildNode((IDocumentElementNode) subitem, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#addSubItem
	 * (int,
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(int index, ISimpleCSSubItemObject subitem) {
		addChildNode((IDocumentElementNode) subitem, index, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getDescription
	 * ()
	 */
	public ISimpleCSDescription getDescription() {
		return (ISimpleCSDescription) getChildNode(ISimpleCSDescription.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getDialog
	 * ()
	 */
	public boolean getDialog() {
		return getBooleanAttributeValue(ATTRIBUTE_DIALOG, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getNextSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem) {
		return (ISimpleCSSubItemObject) getNextSibling(
				(IDocumentElementNode) subitem, ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#
	 * getOnCompletion()
	 */
	public ISimpleCSOnCompletion getOnCompletion() {
		return (ISimpleCSOnCompletion) getChildNode(ISimpleCSOnCompletion.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#
	 * getPreviousSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem) {
		return (ISimpleCSSubItemObject) getPreviousSibling(
				(IDocumentElementNode) subitem, ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getSkip()
	 */
	public boolean getSkip() {
		return getBooleanAttributeValue(ATTRIBUTE_SKIP, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#
	 * getSubItemCount()
	 */
	public int getSubItemCount() {
		return getChildNodeCount(ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getSubItems
	 * ()
	 */
	public ISimpleCSSubItemObject[] getSubItems() {
		ArrayList filteredChildren = getChildNodesList(
				ISimpleCSSubItemObject.class, true);
		return (ISimpleCSSubItemObject[]) filteredChildren
				.toArray(new ISimpleCSSubItemObject[filteredChildren.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#getTitle()
	 */
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#hasSubItems
	 * ()
	 */
	public boolean hasSubItems() {
		return hasChildNodes(ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#indexOfSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public int indexOfSubItem(ISimpleCSSubItemObject subitem) {
		return indexOf((IDocumentElementNode) subitem);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#isFirstSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem) {
		return isFirstChildNode((IDocumentElementNode) subitem,
				ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#isLastSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem) {
		return isLastChildNode((IDocumentElementNode) subitem,
				ISimpleCSSubItemObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#moveSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject,
	 * int)
	 */
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex) {
		moveChildNode((IDocumentElementNode) subitem, newRelativeIndex, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#removeSubItem
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem) {
		removeChildNode((IDocumentElementNode) subitem, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#removeSubItem
	 * (int)
	 */
	public void removeSubItem(int index) {
		removeChildNode(index, ISimpleCSSubItemObject.class, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#setDescription
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSDescription)
	 */
	public void setDescription(ISimpleCSDescription description) {
		setChildNode((IDocumentElementNode) description,
				ISimpleCSDescription.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#setDialog
	 * (boolean)
	 */
	public void setDialog(boolean dialog) {
		setBooleanAttributeValue(ATTRIBUTE_DIALOG, dialog);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#
	 * setOnCompletion
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSOnCompletion)
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion) {
		setChildNode((IDocumentElementNode) onCompletion,
				ISimpleCSOnCompletion.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#setSkip
	 * (boolean)
	 */
	public void setSkip(boolean skip) {
		setBooleanAttributeValue(ATTRIBUTE_SKIP, skip);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSItem#setTitle
	 * (java.lang.String)
	 */
	public void setTitle(String title) {
		setXMLAttribute(ATTRIBUTE_TITLE, title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * getContextId()
	 */
	public String getContextId() {
		return getXMLAttributeValue(ATTRIBUTE_CONTEXTID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * getHref()
	 */
	public String getHref() {
		return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		setXMLAttribute(ATTRIBUTE_CONTEXTID, contextId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSHelpObject#
	 * setHref(java.lang.String)
	 */
	public void setHref(String href) {
		setXMLAttribute(ATTRIBUTE_HREF, href);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRun#getExecutable
	 * ()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		return (ISimpleCSRunContainerObject) getChildNode(ISimpleCSRunContainerObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRun#setExecutable
	 * (
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunContainerObject
	 * )
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		setChildNode((IDocumentElementNode) executable,
				ISimpleCSRunContainerObject.class);
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
		return TYPE_ITEM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.simple.SimpleCSObject#getChildren
	 * ()
	 */
	public List getChildren() {
		// Add subitems
		// Add unsupported perform-when if it is set as the executable
		Class[] classes = { ISimpleCSSubItemObject.class,
				ISimpleCSPerformWhen.class };
		return getChildNodesList(classes, true);
	}

}
