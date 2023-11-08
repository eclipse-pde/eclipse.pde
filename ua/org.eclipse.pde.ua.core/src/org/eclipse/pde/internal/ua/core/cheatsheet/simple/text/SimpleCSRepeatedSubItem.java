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

	@Override
	public ISimpleCSSubItem getSubItem() {
		return (ISimpleCSSubItem) getChildNode(ISimpleCSSubItem.class);
	}

	@Override
	public String getValues() {
		return getXMLAttributeValue(ATTRIBUTE_VALUES);
	}

	@Override
	public void setSubItem(ISimpleCSSubItem subitem) {
		setChildNode(subitem, ISimpleCSSubItem.class);
	}

	@Override
	public void setValues(String values) {
		setXMLAttribute(ATTRIBUTE_VALUES, values);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		// Add subitem
		// TODO: MP: TEO: LOW: Write general method to return first occurrence
		// only?
		return getChildNodesList(ISimpleCSSubItem.class, true);
	}

	@Override
	public String getName() {
		// Leave as is. Not supported in editor UI
		return ELEMENT_REPEATED_SUBITEM;
	}

	@Override
	public int getType() {
		return TYPE_REPEATED_SUBITEM;
	}

}
