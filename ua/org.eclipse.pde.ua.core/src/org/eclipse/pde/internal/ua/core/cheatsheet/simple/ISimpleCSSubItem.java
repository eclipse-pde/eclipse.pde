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

public interface ISimpleCSSubItem extends ISimpleCSSubItemObject, ISimpleCSRun {

	/**
	 * Attribute: label
	 * 
	 * @return
	 */
	public String getLabel();

	/**
	 * Attribute: label
	 * 
	 * @param label
	 */
	public void setLabel(String label);

	/**
	 * Attribute: skip
	 * 
	 * @return
	 */
	public boolean getSkip();

	/**
	 * Attribute: skip
	 * 
	 * @param skip
	 */
	public void setSkip(boolean skip);

	/**
	 * Attribute: when
	 * 
	 * @return
	 */
	public String getWhen();

	/**
	 * Attribute: when
	 * 
	 * @param when
	 */
	public void setWhen(String when);

}
