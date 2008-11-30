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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

public interface ISimpleCSIntro extends ISimpleCSObject, ISimpleCSHelpObject {

	/**
	 * Element: description
	 * 
	 * @return
	 */
	public ISimpleCSDescription getDescription();

	/**
	 * Element: description
	 * 
	 * @param description
	 */
	public void setDescription(ISimpleCSDescription description);

}
