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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

public interface ISimpleCSSubItem extends ISimpleCSSubItemObject, ISimpleCSRun {

	/**
	 * Attribute: label
	 */
	public String getLabel();

	/**
	 * Attribute: label
	 */
	public void setLabel(String label);

	/**
	 * Attribute: skip
	 */
	public boolean getSkip();

	/**
	 * Attribute: skip
	 */
	public void setSkip(boolean skip);

	/**
	 * Attribute: when
	 */
	public String getWhen();

	/**
	 * Attribute: when
	 */
	public void setWhen(String when);

}
