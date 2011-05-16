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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import org.eclipse.core.resources.IProject;

/**
 * IRegisterCSData
 */
public interface IRegisterCSData {

	/**
	 * @return
	 */
	public String getDataDescription();

	/**
	 * @return
	 */
	public String getDataCategoryName();

	/**
	 * @return
	 */
	public String getDataCategoryID();

	/**
	 * @return
	 */
	public int getDataCategoryType();

	/**
	 * @return
	 */
	public String getDataContentFile();

	/**
	 * @return
	 */
	public String getDataCheatSheetID();

	/**
	 * @return
	 */
	public String getDataCheatSheetName();

	/**
	 * @return
	 */
	public boolean isCompositeCheatSheet();

	/**
	 * @return
	 */
	public IProject getPluginProject();

}
