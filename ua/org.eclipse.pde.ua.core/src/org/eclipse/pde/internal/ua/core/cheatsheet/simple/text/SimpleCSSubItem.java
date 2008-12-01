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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#getLabel
	 * ()
	 */
	public String getLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#getSkip
	 * ()
	 */
	public boolean getSkip() {
		return getBooleanAttributeValue(ATTRIBUTE_SKIP, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#getWhen
	 * ()
	 */
	public String getWhen() {
		return getXMLAttributeValue(ATTRIBUTE_WHEN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#setLabel
	 * (java.lang.String)
	 */
	public void setLabel(String label) {
		setXMLAttribute(ATTRIBUTE_LABEL, label);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#setSkip
	 * (boolean)
	 */
	public void setSkip(boolean skip) {
		setBooleanAttributeValue(ATTRIBUTE_SKIP, skip);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSSubItem#setWhen
	 * (java.lang.String)
	 */
	public void setWhen(String when) {
		setXMLAttribute(ATTRIBUTE_WHEN, when);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRun#getExecutable
	 * ()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		return (ISimpleCSRunContainerObject) getChildNode(ISimpleCSRunContainerObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRun#setExecutable
	 * (
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunContainerObject
	 * )
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		setChildNode((IDocumentElementNode) executable,
				ISimpleCSRunContainerObject.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		// TODO: MP: TEO: LOW: Revisit children returned that only can have one
		// - do not return full list
		// Add unsupported perform-when if it is set as the executable
		return getChildNodesList(ISimpleCSPerformWhen.class, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getName
	 * ()
	 */
	public String getName() {
		return getLabel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_SUBITEM;
	}

}
