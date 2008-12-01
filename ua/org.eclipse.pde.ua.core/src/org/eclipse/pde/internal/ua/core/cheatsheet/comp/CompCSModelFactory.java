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

import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;

public class CompCSModelFactory implements ICompCSModelFactory {

	private ICompCSModel fModel;

	/**
	 * @param model
	 */
	public CompCSModelFactory(ICompCSModel model) {
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCS()
	 */
	public ICompCS createCompCS() {
		return new CompCS(fModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSDependency
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSDependency createCompCSDependency(ICompCSObject parent) {
		return new CompCSDependency(fModel, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSIntro
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSIntro createCompCSIntro(ICompCSObject parent) {
		return new CompCSIntro(fModel, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSOnCompletion
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSOnCompletion createCompCSOnCompletion(ICompCSObject parent) {
		return new CompCSOnCompletion(fModel, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSParam
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSParam createCompCSParam(ICompCSObject parent) {
		return new CompCSParam(fModel, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSTask
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSTask createCompCSTask(ICompCSObject parent) {
		return new CompCSTask(fModel, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory#
	 * createCompCSTaskGroup
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject)
	 */
	public ICompCSTaskGroup createCompCSTaskGroup(ICompCSObject parent) {
		return new CompCSTaskGroup(fModel, parent);
	}

}
