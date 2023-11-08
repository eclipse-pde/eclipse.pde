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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;

public class SimpleCSConditionalSubItem extends SimpleCSObject implements
		ISimpleCSConditionalSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSConditionalSubItem(ISimpleCSModel model) {
		super(model, ELEMENT_CONDITIONAL_SUBITEM);
	}

	@Override
	public void addSubItem(ISimpleCSSubItem subitem) {
		addChildNode(subitem, true);
	}

	@Override
	public String getCondition() {
		return getXMLAttributeValue(ATTRIBUTE_CONDITION);
	}

	@Override
	public ISimpleCSSubItem[] getSubItems() {
		List<IDocumentElementNode> filteredChildren = getChildNodesList(ISimpleCSSubItem.class, true);
		return filteredChildren
				.toArray(new ISimpleCSSubItem[filteredChildren.size()]);
	}

	@Override
	public void removeSubItem(ISimpleCSSubItem subitem) {
		removeChildNode(subitem, true);
	}

	@Override
	public void setCondition(String condition) {
		setXMLAttribute(ATTRIBUTE_CONDITION, condition);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return getChildNodesList(ISimpleCSSubItem.class, true);
	}

	@Override
	public String getName() {
		// Leave as is. Not supported in editor UI
		return ELEMENT_CONDITIONAL_SUBITEM;
	}

	@Override
	public int getType() {
		return TYPE_CONDITIONAL_SUBITEM;
	}

}
