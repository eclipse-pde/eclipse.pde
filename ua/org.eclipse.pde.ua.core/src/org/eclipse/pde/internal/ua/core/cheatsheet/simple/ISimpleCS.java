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

public interface ISimpleCS extends ISimpleCSObject {

	/**
	 * Attribute: title
	 */
	public String getTitle();

	/**
	 * Attribute: title
	 */
	public void setTitle(String title);

	/**
	 * Element: intro
	 */
	public ISimpleCSIntro getIntro();

	/**
	 * Element: intro
	 */
	public void setIntro(ISimpleCSIntro intro);

	/**
	 * Element: item
	 */
	public ISimpleCSItem[] getItems();

	/**
	 * Element: item
	 */
	public void addItem(ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public void addItem(int index, ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public void removeItem(ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public void removeItem(int index);

	/**
	 * Element: item
	 */
	public void moveItem(ISimpleCSItem item, int newRelativeIndex);

	/**
	 * Element: item
	 */
	public boolean isFirstItem(ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public boolean isLastItem(ISimpleCSItem item);

	public int indexOfItem(ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public int getItemCount();

	/**
	 * Element: item
	 */
	public boolean hasItems();

	/**
	 * Element: item
	 */
	public ISimpleCSItem getNextSibling(ISimpleCSItem item);

	/**
	 * Element: item
	 */
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item);

}
