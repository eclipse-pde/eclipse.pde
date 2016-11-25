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

	public String getDataDescription();

	public String getDataCategoryName();

	public String getDataCategoryID();

	public int getDataCategoryType();

	public String getDataContentFile();

	public String getDataCheatSheetID();

	public String getDataCheatSheetName();

	public boolean isCompositeCheatSheet();

	public IProject getPluginProject();
}
