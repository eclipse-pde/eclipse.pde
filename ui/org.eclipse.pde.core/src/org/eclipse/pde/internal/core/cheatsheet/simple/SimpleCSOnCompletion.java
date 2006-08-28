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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.w3c.dom.Node;

/**
 * SimpleCSOnCompletion
 *
 */
public class SimpleCSOnCompletion extends SimpleCSObject implements
		ISimpleCSOnCompletion {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSOnCompletion(ISimpleCSModel model) {
		super(model);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion#getContents()
	 */
	public String getContents() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion#setContents(java.lang.String)
	 */
	public void setContents(String contents) {
		// TODO Auto-generated method stub

	}

	public void parse(Node node) {
		// TODO Auto-generated method stub
		
	}

	public void write(String indent, PrintWriter writer) {
		// TODO Auto-generated method stub
		
	}

}
