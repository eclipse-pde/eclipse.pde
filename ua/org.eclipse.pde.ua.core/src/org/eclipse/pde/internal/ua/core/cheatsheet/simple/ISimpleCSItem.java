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

public interface ISimpleCSItem extends ISimpleCSObject, ISimpleCSHelpObject,
		ISimpleCSRun {

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
	 * Attribute: dialog
	 * 
	 * @return
	 */
	public boolean getDialog();

	/**
	 * Attribute: dialog
	 * 
	 * @param dialog
	 */
	public void setDialog(boolean dialog);

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
	 * Elements: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @return
	 */
	public ISimpleCSSubItemObject[] getSubItems();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param index
	 * @param subitem
	 */
	public void addSubItem(int index, ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param index
	 */
	public void removeSubItem(int index);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @param newRelativeIndex
	 */
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex);

	/**
	 * Element: onCompletion
	 * 
	 * @return
	 */
	public ISimpleCSOnCompletion getOnCompletion();

	/**
	 * Element: onCompletion
	 * 
	 * @param onCompletion
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @return
	 */
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @return
	 */
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @return
	 */
	public int indexOfSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @return
	 */
	public int getSubItemCount();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @return
	 */
	public boolean hasSubItems();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @return
	 */
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 * 
	 * @param subitem
	 * @return
	 */
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem);

}
