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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSCommand#getReturns
	 * ()
	 */
	public String getReturns() {
		return getXMLAttributeValue(ATTRIBUTE_RETURNS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSCommand#
	 * getSerialization()
	 */
	public String getSerialization() {
		return getXMLAttributeValue(ATTRIBUTE_SERIALIZATION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSCommand#setReturns
	 * (java.lang.String)
	 */
	public void setReturns(String returns) {
		setXMLAttribute(ATTRIBUTE_RETURNS, returns);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSCommand#
	 * setSerialization(java.lang.String)
	 */
	public void setSerialization(String serialization) {
		setXMLAttribute(ATTRIBUTE_SERIALIZATION, serialization);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getName
	 * ()
	 */
	public String getName() {
		// Leave as is. Not a separate node in tree view
		return ELEMENT_COMMAND;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_COMMAND;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isLeafNode()
	 */
	public boolean isLeafNode() {
		return true;
	}

}
