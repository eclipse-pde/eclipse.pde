/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

public interface ISimpleCSRunObject extends ISimpleCSRunContainerObject {

	/**
	 * Attribute: confirm
	 */
	public boolean getConfirm();

	/**
	 * Attribute: confirm
	 */
	public void setConfirm(boolean confirm);

	/**
	 * Attribute: when
	 */
	public String getWhen();

	/**
	 * Attribute: when
	 */
	public void setWhen(String when);

	/**
	 * Attribute: translate
	 */
	public String getTranslate();

	/**
	 * Attribute: translate
	 */
	public void setTranslate(String translate);

	/**
	 * Attribute: required
	 */
	public boolean getRequired();

	/**
	 * Attribute: required
	 */
	public void setRequired(boolean required);

}
