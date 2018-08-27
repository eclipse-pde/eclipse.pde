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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunObject;

public class SimpleCSPerformWhen extends SimpleCSObject implements
		ISimpleCSPerformWhen {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSPerformWhen(ISimpleCSModel model) {
		super(model, ELEMENT_PERFORM_WHEN);
	}

	@Override
	public void addExecutable(ISimpleCSRunObject executable) {
		addChildNode(executable, true);
	}

	@Override
	public String getCondition() {
		return getXMLAttributeValue(ATTRIBUTE_CONDITION);
	}

	@Override
	public ISimpleCSRunObject[] getExecutables() {
		List<IDocumentElementNode> filteredChildren = getChildNodesList(ISimpleCSRunObject.class, true);
		return filteredChildren
				.toArray(new ISimpleCSRunObject[filteredChildren.size()]);
	}

	@Override
	public void removeExecutable(ISimpleCSRunObject executable) {
		removeChildNode(executable, true);
	}

	@Override
	public void setCondition(String condition) {
		setXMLAttribute(ATTRIBUTE_CONDITION, condition);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		// Leave as is. Not supported in editor UI
		return ELEMENT_PERFORM_WHEN;
	}

	@Override
	public int getType() {
		return TYPE_PERFORM_WHEN;
	}

}
