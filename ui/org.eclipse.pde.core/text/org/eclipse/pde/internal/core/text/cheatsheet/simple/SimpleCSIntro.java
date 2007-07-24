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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;

/**
 * SimpleCSIntro
 *
 */
public class SimpleCSIntro extends SimpleCSObject implements ISimpleCSIntro {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSIntro(ISimpleCSModel model) {
		super(model);
		// TODO: MP: CURRENT: IMPLEMENT
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getDescription()
	 */
	public ISimpleCSDescription getDescription() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setDescription(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription)
	 */
	public void setDescription(ISimpleCSDescription description) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#getContextId()
	 */
	public String getContextId() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#getHref()
	 */
	public String getHref() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#setHref(java.lang.String)
	 */
	public void setHref(String href) {
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
