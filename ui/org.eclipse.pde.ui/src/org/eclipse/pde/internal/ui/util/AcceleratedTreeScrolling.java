/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sascha Becher <s.becher@qualitype.de> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Accelerated tree scrolling with the mouse wheel during which MOD1 (Ctrl) key modifier has been pressed
 */
public class AcceleratedTreeScrolling implements MouseWheelListener {

	private Tree fTree;
	private int fSkipLines;

	/**
	 *  Accelerated mouse wheel scrolling during which the MOD1 (Ctrl) key modifier has been pressed
	 *  
	 * @param tree tree to scroll
	 * @param skipLines lines to scroll
	 */
	public AcceleratedTreeScrolling(Tree tree, int skipLines) {
		fTree = tree;
		fSkipLines = (skipLines > 1) ? skipLines : 2;
	}

	public void mouseScrolled(MouseEvent e) {
		// scroll only when MOD1 is pressed
		if ((e.stateMask & SWT.MOD1) == SWT.MOD1 && fTree != null) {
			TreeItem item = fTree.getTopItem();
			if (item != null) {
				TreeItem nextItem = item;
				for (int i = 0; i < fSkipLines; i++) {
					TreeItem foundItem = null;
					if (e.count < 0) // determines scrolling direction
						foundItem = NextItem(fTree, nextItem);
					else
						foundItem = PreviousItem(fTree, nextItem);
					if (foundItem == null) {
						break;
					}
					nextItem = foundItem;
				}
				fTree.setTopItem(nextItem);
			}
		}
	}

	TreeItem PreviousItem(Tree tree, TreeItem item) {
		if (item == null)
			return null;
		TreeItem childItem = item;
		TreeItem parentItem = childItem.getParentItem();
		int index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
		if (index == 0) {
			return parentItem;
		}
		TreeItem previousItem = parentItem == null ? tree.getItem(index - 1) : parentItem.getItem(index - 1);
		int count = previousItem.getItemCount();
		while (count > 0 && previousItem.getExpanded()) {
			previousItem = previousItem.getItem(count - 1);
			count = previousItem.getItemCount();
		}
		return previousItem;
	}

	TreeItem NextItem(Tree tree, TreeItem item) {
		if (item == null)
			return null;
		if (item.getExpanded()) {
			if (item.getItemCount() > 0) {
				return item.getItem(0);
			}
		}
		TreeItem childItem = item;
		TreeItem parentItem = childItem.getParentItem();
		int index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
		int count = parentItem == null ? tree.getItemCount() : parentItem.getItemCount();
		while (true) {
			if (index + 1 < count) {
				return parentItem == null ? tree.getItem(index + 1) : parentItem.getItem(index + 1);
			}
			if (parentItem == null) {
				return null;
			}
			childItem = parentItem;
			parentItem = childItem.getParentItem();
			index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
			count = parentItem == null ? tree.getItemCount() : parentItem.getItemCount();
		}
	}

}