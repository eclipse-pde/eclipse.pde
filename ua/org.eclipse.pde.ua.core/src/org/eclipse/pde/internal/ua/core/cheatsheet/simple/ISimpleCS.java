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

public interface ISimpleCS extends ISimpleCSObject {

	/**
	 * Attribute: title
	 * 
	 * @return
	 */
	public String getTitle();

	/**
	 * Attribute: title
	 * 
	 * @param title
	 */
	public void setTitle(String title);

	/**
	 * Element: intro
	 * 
	 * @return
	 */
	public ISimpleCSIntro getIntro();

	/**
	 * Element: intro
	 * 
	 * @param intro
	 */
	public void setIntro(ISimpleCSIntro intro);

	/**
	 * Element: item
	 * 
	 * @return
	 */
	public ISimpleCSItem[] getItems();

	/**
	 * Element: item
	 * 
	 * @param item
	 */
	public void addItem(ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @param item
	 * @param index
	 */
	public void addItem(int index, ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @param item
	 */
	public void removeItem(ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @param index
	 */
	public void removeItem(int index);

	/**
	 * Element: item
	 * 
	 * @param item
	 * @param newRelativeIndex
	 */
	public void moveItem(ISimpleCSItem item, int newRelativeIndex);

	/**
	 * Element: item
	 * 
	 * @param item
	 */
	public boolean isFirstItem(ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @param item
	 */
	public boolean isLastItem(ISimpleCSItem item);

	/**
	 * @param item
	 * @return
	 */
	public int indexOfItem(ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @return
	 */
	public int getItemCount();

	/**
	 * Element: item
	 * 
	 * @return
	 */
	public boolean hasItems();

	/**
	 * Element: item
	 * 
	 * @param item
	 * @return
	 */
	public ISimpleCSItem getNextSibling(ISimpleCSItem item);

	/**
	 * Element: item
	 * 
	 * @param item
	 * @return
	 */
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item);

}
