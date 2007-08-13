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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;

/**
 * SimpleCSCommand
 *
 */
public class SimpleCSCommand extends SimpleCSObject implements ISimpleCSCommand {

	private static final long serialVersionUID = 1L;

	// TODO: MP: TEO: HIGH: Verify translate attribute values okay - no translate before
	// TODO: MP: SimpleCS:  LOW: Implement 'required' attribute 
	
	/**
	 * @param model
	 */
	public SimpleCSCommand(ISimpleCSModel model) {
		super(model, ELEMENT_COMMAND);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getReturns()
	 */
	public String getReturns() {
		return getXMLAttributeValue(ATTRIBUTE_RETURNS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getSerialization()
	 */
	public String getSerialization() {
		return getXMLAttributeValue(ATTRIBUTE_SERIALIZATION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setReturns(java.lang.String)
	 */
	public void setReturns(String returns) {
		setXMLAttribute(ATTRIBUTE_RETURNS, returns);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setSerialization(java.lang.String)
	 */
	public void setSerialization(String serialization) {
		setXMLAttribute(ATTRIBUTE_SERIALIZATION, serialization);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getConfirm()
	 */
	public boolean getConfirm() {
		return getBooleanAttributeValue(ATTRIBUTE_CONFIRM, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getTranslate()
	 */
	public String getTranslate() {
		return getXMLAttributeValue(ATTRIBUTE_TRANSLATE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getWhen()
	 */
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		setBooleanAttributeValue(ATTRIBUTE_CONFIRM, confirm);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setTranslate(java.lang.String)
	 */
	public void setTranslate(String translate) {
		setXMLAttribute(ATTRIBUTE_TRANSLATE, translate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not a separate node in tree view
		return ELEMENT_COMMAND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_COMMAND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isLeafNode()
	 */
	public boolean isLeafNode() {
		return true;
	}	
	
}
