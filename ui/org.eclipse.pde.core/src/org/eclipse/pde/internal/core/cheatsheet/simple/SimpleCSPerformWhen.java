/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject;
import org.w3c.dom.Element;

/**
 * SimpleCSPerformWhen
 *
 */
public class SimpleCSPerformWhen extends SimpleCSObject implements
		ISimpleCSPerformWhen {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSPerformWhen(ISimpleCSModel model) {
		super(model);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#addExecutables(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject[])
	 */
	public void addExecutables(ISimpleCSRunObject[] executables) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getCondition()
	 */
	public String getCondition() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getExecutables()
	 */
	public ISimpleCSRunObject[] getExecutables() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#removeExecutables(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject[])
	 */
	public void removeExecutables(ISimpleCSRunObject[] executables) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#setCondition(java.lang.String)
	 */
	public void setCondition(String condition) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_PERFORM_WHEN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// TODO: MP: Update name
		return ELEMENT_PERFORM_WHEN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		// TODO Auto-generated method stub
		return new ArrayList();
	}

}
