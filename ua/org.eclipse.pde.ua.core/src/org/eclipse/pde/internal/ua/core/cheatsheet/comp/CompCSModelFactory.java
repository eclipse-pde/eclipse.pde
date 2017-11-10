/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;

public class CompCSModelFactory implements ICompCSModelFactory {

	private ICompCSModel fModel;

	/**
	 * @param model
	 */
	public CompCSModelFactory(ICompCSModel model) {
		fModel = model;
	}

	@Override
	public ICompCS createCompCS() {
		return new CompCS(fModel);
	}

	@Override
	public ICompCSDependency createCompCSDependency(ICompCSObject parent) {
		return new CompCSDependency(fModel, parent);
	}

	@Override
	public ICompCSIntro createCompCSIntro(ICompCSObject parent) {
		return new CompCSIntro(fModel, parent);
	}

	@Override
	public ICompCSOnCompletion createCompCSOnCompletion(ICompCSObject parent) {
		return new CompCSOnCompletion(fModel, parent);
	}

	@Override
	public ICompCSParam createCompCSParam(ICompCSObject parent) {
		return new CompCSParam(fModel, parent);
	}

	@Override
	public ICompCSTask createCompCSTask(ICompCSObject parent) {
		return new CompCSTask(fModel, parent);
	}

	@Override
	public ICompCSTaskGroup createCompCSTaskGroup(ICompCSObject parent) {
		return new CompCSTaskGroup(fModel, parent);
	}

}
