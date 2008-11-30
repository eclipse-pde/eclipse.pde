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

public interface ISimpleCSConditionalSubItem extends ISimpleCSSubItemObject {

	/**
	 * Attribute: condition
	 * 
	 * @return
	 */
	public String getCondition();

	/**
	 * Attribute: condition
	 * 
	 * @param condition
	 */
	public void setCondition(String condition);

	/**
	 * Element: subitem
	 * 
	 * @return
	 */
	public ISimpleCSSubItem[] getSubItems();

	/**
	 * Elements: subitem
	 * 
	 * @param subitems
	 */
	public void addSubItem(ISimpleCSSubItem subitem);

	/**
	 * Elements: subitem
	 * 
	 * @param subitems
	 */
	public void removeSubItem(ISimpleCSSubItem subitem);

}
