/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import org.eclipse.core.resources.IProject;

public interface IRegisterTOCData {

	/**
	 * @return
	 */
	public boolean getDataPrimary();

	/**
	 * @return
	 */
	public String getDataTocFile();

	/**
	 * @return
	 */
	public IProject getPluginProject();

}
