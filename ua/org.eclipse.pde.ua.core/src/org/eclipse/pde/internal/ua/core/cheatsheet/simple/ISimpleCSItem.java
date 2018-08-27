/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

public interface ISimpleCSItem extends ISimpleCSHelpObject, ISimpleCSRun {

	/**
	 * Element: description
	 */
	public ISimpleCSDescription getDescription();

	/**
	 * Element: description
	 */
	public void setDescription(ISimpleCSDescription description);

	/**
	 * Attribute: title
	 */
	public String getTitle();

	/**
	 * Attribute: title
	 */
	public void setTitle(String title);

	/**
	 * Attribute: dialog
	 */
	public boolean getDialog();

	/**
	 * Attribute: dialog
	 */
	public void setDialog(boolean dialog);

	/**
	 * Attribute: skip
	 */
	public boolean getSkip();

	/**
	 * Attribute: skip
	 */
	public void setSkip(boolean skip);

	/**
	 * Elements: subitem, repeated-subitem, conditional-subitem
	 */
	public ISimpleCSSubItemObject[] getSubItems();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public void addSubItem(int index, ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public void removeSubItem(int index);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex);

	/**
	 * Element: onCompletion
	 */
	public ISimpleCSOnCompletion getOnCompletion();

	/**
	 * Element: onCompletion
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public int indexOfSubItem(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public int getSubItemCount();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public boolean hasSubItems();

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem);

	/**
	 * Element: subitem, repeated-subitem, conditional-subitem
	 */
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem);

}
