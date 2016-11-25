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

	public ICompCS createCompCS();

	public ICompCSTaskGroup createCompCSTaskGroup(ICompCSObject parent);

	public ICompCSTask createCompCSTask(ICompCSObject parent);

	public ICompCSIntro createCompCSIntro(ICompCSObject parent);

	public ICompCSOnCompletion createCompCSOnCompletion(ICompCSObject parent);

	public ICompCSDependency createCompCSDependency(ICompCSObject parent);

	public ICompCSParam createCompCSParam(ICompCSObject parent);

}
