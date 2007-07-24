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

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;

/**
 * SimpleCSRepeatedSubItem
 *
 */
public class SimpleCSRepeatedSubItem extends SimpleCSObject implements
		ISimpleCSRepeatedSubItem {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSRepeatedSubItem(ISimpleCSModel model) {
		super(model);
		// TODO: MP: CURRENT: IMPLEMENT
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getSubItem()
	 */
	public ISimpleCSSubItem getSubItem() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getValues()
	 */
	public String getValues() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void setSubItem(ISimpleCSSubItem subitem) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setValues(java.lang.String)
	 */
	public void setValues(String values) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	public List getChildren() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public String getName() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public int getType() {
		// TODO: MP: CURRENT: IMPLEMENT
		return 0;
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: CURRENT: IMPLEMENT
		
	}

}
