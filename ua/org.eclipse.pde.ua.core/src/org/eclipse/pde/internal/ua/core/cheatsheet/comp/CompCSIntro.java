/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;

public class CompCSIntro extends CompCSDataObject implements ICompCSIntro {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSIntro(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSDataObject#getElement
	 * ()
	 */
	public String getElement() {
		return ELEMENT_INTRO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSDataObject#getType()
	 */
	public int getType() {
		return TYPE_INTRO;
	}

}
