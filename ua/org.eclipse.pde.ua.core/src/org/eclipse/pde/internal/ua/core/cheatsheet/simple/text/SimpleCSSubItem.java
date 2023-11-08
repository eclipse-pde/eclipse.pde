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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;

public class SimpleCSSubItem extends SimpleCSObject implements ISimpleCSSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSSubItem(ISimpleCSModel model) {
		super(model, ELEMENT_SUBITEM);
	}

	@Override
	public String getLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	@Override
	public boolean getSkip() {
		return getBooleanAttributeValue(ATTRIBUTE_SKIP, false);
	}

	@Override
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	@Override
	public void setLabel(String label) {
		setXMLAttribute(ATTRIBUTE_LABEL, label);
	}

	@Override
	public void setSkip(boolean skip) {
		setBooleanAttributeValue(ATTRIBUTE_SKIP, skip);
	}

	@Override
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
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
	public List<IDocumentElementNode> getChildren() {
		// TODO: MP: TEO: LOW: Revisit children returned that only can have one
		// - do not return full list
		// Add unsupported perform-when if it is set as the executable
		return getChildNodesList(ISimpleCSPerformWhen.class, true);
	}

	@Override
	public String getName() {
		return getLabel();
	}

	@Override
	public int getType() {
		return TYPE_SUBITEM;
	}

}
