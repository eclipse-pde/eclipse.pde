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

public interface ISimpleCSConditionalSubItem extends ISimpleCSSubItemObject {

	/**
	 * Attribute: condition
	 */
	public String getCondition();

	/**
	 * Attribute: condition
	 */
	public void setCondition(String condition);

	/**
	 * Element: subitem
	 */
	public ISimpleCSSubItem[] getSubItems();

	/**
	 * Elements: subitem
	 */
	public void addSubItem(ISimpleCSSubItem subitem);

	/**
	 * Elements: subitem
	 */
	public void removeSubItem(ISimpleCSSubItem subitem);

}
