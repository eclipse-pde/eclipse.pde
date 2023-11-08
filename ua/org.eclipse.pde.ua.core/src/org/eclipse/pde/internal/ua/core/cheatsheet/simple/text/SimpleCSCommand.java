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
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;

public class SimpleCSCommand extends SimpleCSRunObject implements
		ISimpleCSCommand {

	private static final long serialVersionUID = 1L;

	// TODO: MP: TEO: HIGH: Verify translate attribute values okay - no
	// translate before

	/**
	 * @param model
	 */
	public SimpleCSCommand(ISimpleCSModel model) {
		super(model, ELEMENT_COMMAND);
	}

	@Override
	public String getReturns() {
		return getXMLAttributeValue(ATTRIBUTE_RETURNS);
	}

	@Override
	public String getSerialization() {
		return getXMLAttributeValue(ATTRIBUTE_SERIALIZATION);
	}

	@Override
	public void setReturns(String returns) {
		setXMLAttribute(ATTRIBUTE_RETURNS, returns);
	}

	@Override
	public void setSerialization(String serialization) {
		setXMLAttribute(ATTRIBUTE_SERIALIZATION, serialization);
	}

	@Override
	public List<IDocumentElementNode> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		// Leave as is. Not a separate node in tree view
		return ELEMENT_COMMAND;
	}

	@Override
	public int getType() {
		return TYPE_COMMAND;
	}

	@Override
	public boolean isLeafNode() {
		return true;
	}

}
