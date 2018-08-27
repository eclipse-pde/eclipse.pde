/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
