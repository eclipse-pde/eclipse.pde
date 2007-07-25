/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;

/**
 * SimpleCSItem
 *
 */
public class SimpleCSItem extends SimpleCSObject implements ISimpleCSItem {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSItem(ISimpleCSModel model) {
		super(model, ELEMENT_ITEM);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#addSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#addSubItem(int, org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(int index, ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDescription()
	 */
	public ISimpleCSDescription getDescription() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDialog()
	 */
	public boolean getDialog() {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getNextSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getOnCompletion()
	 */
	public ISimpleCSOnCompletion getOnCompletion() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getPreviousSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSkip()
	 */
	public boolean getSkip() {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSubItemCount()
	 */
	public int getSubItemCount() {
		// TODO: MP: CURRENT: IMPLEMENT
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSubItems()
	 */
	public ISimpleCSSubItemObject[] getSubItems() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getTitle()
	 */
	public String getTitle() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#hasSubItems()
	 */
	public boolean hasSubItems() {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#indexOfSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public int indexOfSubItem(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#isFirstSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#isLastSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#moveSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject, int)
	 */
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItem(int)
	 */
	public void removeSubItem(int index) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDescription(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription)
	 */
	public void setDescription(ISimpleCSDescription description) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDialog(boolean)
	 */
	public void setDialog(boolean dialog) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setOnCompletion(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion)
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setSkip(boolean)
	 */
	public void setSkip(boolean skip) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#getContextId()
	 */
	public String getContextId() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#getHref()
	 */
	public String getHref() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSHelpObject#setHref(java.lang.String)
	 */
	public void setHref(String href) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRun#getExecutable()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRun#setExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject)
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	public List getChildren() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public String getName() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public int getType() {
		// TODO: MP: CURRENT: IMPLEMENT
		return 0;
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: CURRENT: IMPLEMENT
		
	}

}
