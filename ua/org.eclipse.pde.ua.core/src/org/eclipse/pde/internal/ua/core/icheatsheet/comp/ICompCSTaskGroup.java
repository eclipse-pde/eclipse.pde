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

public interface ICompCSTaskGroup extends ICompCSTaskObject {

	/**
	 * Elements: taskGroup, task
	 */
	public void addFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public void addFieldTaskObject(int index, ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public void removeFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public void moveFieldTaskObject(ICompCSTaskObject taskObject,
			int newRelativeIndex);

	/**
	 * Elements: taskGroup, task
	 */
	public void removeFieldTaskObject(int index);

	/**
	 * Elements: taskGroup, task
	 */
	public ICompCSTaskObject[] getFieldTaskObjects();

	/**
	 * Elements: taskGroup, task
	 */
	public boolean isFirstFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public boolean isLastFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public int indexOfFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public int getFieldTaskObjectCount();

	/**
	 * Elements: taskGroup, task
	 */
	public boolean hasFieldTaskObjects();

	/**
	 * Elements: taskGroup, task
	 */
	public ICompCSTaskObject getNextSibling(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 */
	public ICompCSTaskObject getPreviousSibling(ICompCSTaskObject taskObject);

}
