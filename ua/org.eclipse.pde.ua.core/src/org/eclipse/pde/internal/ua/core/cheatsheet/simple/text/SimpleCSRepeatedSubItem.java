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

import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;

public class SimpleCSRepeatedSubItem extends SimpleCSObject implements
		ISimpleCSRepeatedSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSRepeatedSubItem(ISimpleCSModel model) {
		super(model, ELEMENT_REPEATED_SUBITEM);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRepeatedSubItem
	 * #getSubItem()
	 */
	public ISimpleCSSubItem getSubItem() {
		return (ISimpleCSSubItem) getChildNode(ISimpleCSSubItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRepeatedSubItem
	 * #getValues()
	 */
	public String getValues() {
		return getXMLAttributeValue(ATTRIBUTE_VALUES);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRepeatedSubItem
	 * #
	 * setSubItem(org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem
	 * )
	 */
	public void setSubItem(ISimpleCSSubItem subitem) {
		setChildNode((IDocumentElementNode) subitem, ISimpleCSSubItem.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRepeatedSubItem
	 * #setValues(java.lang.String)
	 */
	public void setValues(String values) {
		setXMLAttribute(ATTRIBUTE_VALUES, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		// Add subitem
		// TODO: MP: TEO: LOW: Write general method to return first occurrence
		// only?
		return getChildNodesList(ISimpleCSSubItem.class, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getName
	 * ()
	 */
	public String getName() {
		// Leave as is. Not supported in editor UI
		return ELEMENT_REPEATED_SUBITEM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_REPEATED_SUBITEM;
	}

}
