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

import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.text.IDocumentNode;

/**
 * SimpleCSRepeatedSubItem
 *
 */
public class SimpleCSRepeatedSubItem extends SimpleCSObject implements
		ISimpleCSRepeatedSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSRepeatedSubItem(ISimpleCSModel model) {
		super(model, ELEMENT_REPEATED_SUBITEM);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getSubItem()
	 */
	public ISimpleCSSubItem getSubItem() {
		return (ISimpleCSSubItem)getChildNode(ISimpleCSSubItem.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getValues()
	 */
	public String getValues() {
		return getXMLAttributeValue(ATTRIBUTE_VALUES);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void setSubItem(ISimpleCSSubItem subitem) {
		setChildNode((IDocumentNode)subitem, ISimpleCSSubItem.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setValues(java.lang.String)
	 */
	public void setValues(String values) {
		setXMLAttribute(ATTRIBUTE_VALUES, values);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		// Add subitem
		// TODO: MP: TEO: Write general method to return first occurrence only?
		return getChildNodesList(ISimpleCSSubItem.class, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not supported in editor UI		
		return ELEMENT_REPEATED_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_REPEATED_SUBITEM;
	}

}
