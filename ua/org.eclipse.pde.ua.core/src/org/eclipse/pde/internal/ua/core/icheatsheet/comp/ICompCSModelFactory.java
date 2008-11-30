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

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

public interface ICompCSModelFactory {

	/**
	 * @return
	 */
	public ICompCS createCompCS();

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSTaskGroup createCompCSTaskGroup(ICompCSObject parent);

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSTask createCompCSTask(ICompCSObject parent);

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSIntro createCompCSIntro(ICompCSObject parent);

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSOnCompletion createCompCSOnCompletion(ICompCSObject parent);

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSDependency createCompCSDependency(ICompCSObject parent);

	/**
	 * @param parent
	 * @return
	 */
	public ICompCSParam createCompCSParam(ICompCSObject parent);

}
