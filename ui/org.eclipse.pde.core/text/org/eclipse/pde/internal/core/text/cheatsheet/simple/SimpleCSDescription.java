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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;

/**
 * SimpleCSDescription
 *
 */
public class SimpleCSDescription extends SimpleCSObject implements
		ISimpleCSDescription {

	// TODO: MP: TEO: Should extend DocumentTextNode
	
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSDescription(ISimpleCSModel model) {
		super(model);
		// TODO: MP: CURRENT: IMPLEMENT
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription#getContent()
	 */
	public String getContent() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription#setContent(java.lang.String)
	 */
	public void setContent(String content) {
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
