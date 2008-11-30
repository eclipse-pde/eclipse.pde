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

public interface ICompCS extends ICompCSObject {

	/**
	 * Attribute: name
	 * 
	 * @param name
	 */
	public void setFieldName(String name);

	/**
	 * Attribute: name
	 * 
	 * @return
	 */
	public String getFieldName();

	/**
	 * Elements: taskGroup, task
	 * 
	 * @param taskObject
	 */
	public void setFieldTaskObject(ICompCSTaskObject taskObject);

	/**
	 * Elements: taskGroup, task
	 * 
	 * @return
	 */
	public ICompCSTaskObject getFieldTaskObject();

}
