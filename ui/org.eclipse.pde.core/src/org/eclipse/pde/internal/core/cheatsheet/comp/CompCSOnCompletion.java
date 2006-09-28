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

package org.eclipse.pde.internal.core.cheatsheet.comp;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;

/**
 * CompCSOnCompletion
 *
 */
public class CompCSOnCompletion extends CompCSDataObject implements
		ICompCSOnCompletion {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSOnCompletion(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSDataObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_ONCOMPLETION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSDataObject#getType()
	 */
	public int getType() {
		return TYPE_ONCOMPLETION;
	}

}
