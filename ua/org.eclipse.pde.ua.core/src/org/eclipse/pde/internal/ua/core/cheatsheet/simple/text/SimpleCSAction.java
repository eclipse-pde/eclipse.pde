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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;

public class SimpleCSAction extends SimpleCSRunObject implements
		ISimpleCSAction {

	private static final long serialVersionUID = 1L;

	private static final int F_MAX_PARAMS = 9;

	// TODO: MP: TEO: MED: Verify translate of paramaters on write is okay - no
	// translate before

	/**
	 * @param model
	 */
	public SimpleCSAction(ISimpleCSModel model) {
		super(model, ELEMENT_ACTION);
	}

	@Override
	public String getClazz() {
		return getXMLAttributeValue(ATTRIBUTE_CLASS);
	}

	@Override
	public String getParam(int index) {
		// Ensure in valid range
		if ((index < 1) || (index > F_MAX_PARAMS)) {
			return null;
		}
		StringBuilder buffer = new StringBuilder(ATTRIBUTE_PARAM);
		buffer.append(index);
		// Get paramN
		return getXMLAttributeValue(buffer.toString());
	}

	@Override
	public String[] getParams() {
		List<String> list = new ArrayList<>();
		// Get all set parameters
		for (int i = 1; i <= F_MAX_PARAMS; i++) {
			String parameter = getParam(i);
			if (parameter == null) {
				break;
			}
			list.add(parameter);
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	public String getPluginId() {
		return getXMLAttributeValue(ATTRIBUTE_PLUGINID);
	}

	@Override
	public void setClazz(String clazz) {
		setXMLAttribute(ATTRIBUTE_CLASS, clazz);
	}

	@Override
	public void setParam(String param, int index) {
		// Ensure proper index
		if ((index < 1) || (index > F_MAX_PARAMS)) {
			return;
		}
		StringBuilder buffer = new StringBuilder(ATTRIBUTE_PARAM);
		buffer.append(index);
		setXMLAttribute(buffer.toString(), param);
	}

	@Override
	public void setPluginId(String pluginId) {
		setXMLAttribute(ATTRIBUTE_PLUGINID, pluginId);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		// Leave as is. Not a separate node in tree view
		return ELEMENT_ACTION;
	}

	@Override
	public int getType() {
		return TYPE_ACTION;
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
