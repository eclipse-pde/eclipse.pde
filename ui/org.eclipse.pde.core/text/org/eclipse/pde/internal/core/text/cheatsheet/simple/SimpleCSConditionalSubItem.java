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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

/**
 * SimpleCSConditionalSubItem
 *
 */
public class SimpleCSConditionalSubItem extends SimpleCSObject implements ISimpleCSConditionalSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSConditionalSubItem(ISimpleCSModel model) {
		super(model, ELEMENT_CONDITIONAL_SUBITEM);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#addSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void addSubItem(ISimpleCSSubItem subitem) {
		addChildNode((IDocumentElementNode)subitem, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#getCondition()
	 */
	public String getCondition() {
		return getXMLAttributeValue(ATTRIBUTE_CONDITION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#getSubItems()
	 */
	public ISimpleCSSubItem[] getSubItems() {
		ArrayList filteredChildren = getChildNodesList(ISimpleCSSubItem.class, true);
		return (ISimpleCSSubItem[])filteredChildren.toArray(new ISimpleCSSubItem[filteredChildren.size()]);	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#removeSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void removeSubItem(ISimpleCSSubItem subitem) {
		removeChildNode((IDocumentElementNode)subitem, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#setCondition(java.lang.String)
	 */
	public void setCondition(String condition) {
		setXMLAttribute(ATTRIBUTE_CONDITION, condition);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getChildren()
	 */
	public List getChildren() {
		// Add subitems
		return getChildNodesList(ISimpleCSSubItem.class, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not supported in editor UI
		return ELEMENT_CONDITIONAL_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_CONDITIONAL_SUBITEM;
	}

}
