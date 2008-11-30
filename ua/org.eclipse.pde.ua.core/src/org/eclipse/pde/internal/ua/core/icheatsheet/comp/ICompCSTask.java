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

public interface ICompCSTask extends ICompCSTaskObject {

	/**
	 * Element: param
	 * 
	 * @param param
	 */
	public void addFieldParam(ICompCSParam param);

	/**
	 * Element: param
	 * 
	 * @param param
	 */
	public void removeFieldParam(ICompCSParam param);

	/**
	 * Element: param
	 * 
	 * @return
	 */
	public ICompCSParam[] getFieldParams();

	/**
	 * Element: param
	 * 
	 * @return
	 */
	public boolean hasFieldParams();

	/**
	 * Element: param
	 * 
	 * @param name
	 * @return
	 */
	public ICompCSParam getFieldParam(String name);

}
