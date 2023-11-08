/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public String getElement() {
		return ELEMENT_INTRO;
	}

	@Override
	public int getType() {
		return TYPE_INTRO;
	}

}
