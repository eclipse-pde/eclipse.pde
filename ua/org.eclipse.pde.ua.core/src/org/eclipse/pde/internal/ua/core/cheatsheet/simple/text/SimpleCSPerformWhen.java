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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSPerformWhen
	 * #addExecutable
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void addExecutable(ISimpleCSRunObject executable) {
		addChildNode((IDocumentElementNode) executable, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSPerformWhen
	 * #getCondition()
	 */
	public String getCondition() {
		return getXMLAttributeValue(ATTRIBUTE_CONDITION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSPerformWhen
	 * #getExecutables()
	 */
	public ISimpleCSRunObject[] getExecutables() {
		ArrayList filteredChildren = getChildNodesList(
				ISimpleCSRunObject.class, true);
		return (ISimpleCSRunObject[]) filteredChildren
				.toArray(new ISimpleCSRunObject[filteredChildren.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSPerformWhen
	 * #removeExecutable
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void removeExecutable(ISimpleCSRunObject executable) {
		removeChildNode((IDocumentElementNode) executable, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSPerformWhen
	 * #setCondition(java.lang.String)
	 */
	public void setCondition(String condition) {
		setXMLAttribute(ATTRIBUTE_CONDITION, condition);
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
		// Leave as is. Not supported in editor UI
		return ELEMENT_PERFORM_WHEN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_PERFORM_WHEN;
	}

}
