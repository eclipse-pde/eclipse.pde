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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject;

/**
 * SimpleCSPerformWhen
 *
 */
public class SimpleCSPerformWhen extends SimpleCSObject implements
		ISimpleCSPerformWhen {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSPerformWhen(ISimpleCSModel model) {
		super(model);
		// TODO: MP: CURRENT: IMPLEMENT
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#addExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void addExecutable(ISimpleCSRunObject executable) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getCondition()
	 */
	public String getCondition() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getExecutables()
	 */
	public ISimpleCSRunObject[] getExecutables() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#removeExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void removeExecutable(ISimpleCSRunObject executable) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#setCondition(java.lang.String)
	 */
	public void setCondition(String condition) {
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
