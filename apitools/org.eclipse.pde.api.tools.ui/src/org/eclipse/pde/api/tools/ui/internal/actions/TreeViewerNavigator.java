/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.api.tools.ui.internal.actions.DeltaSession.TreeNode;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Navigation in the tree viewer.
 */
class TreeViewerNavigator {
	private final TreeViewer fViewer;

	public TreeViewerNavigator(TreeViewer viewer) {
		fViewer = viewer;
	}

	public void navigateNext(boolean forward) {
		TreeItem currentItem = getCurrentItem(forward);
		if (currentItem == null)
			return;
		TreeItem nextItem =  null;
		if (forward) {
			nextItem = getNextItemForward(currentItem);
			if (nextItem == null)
				nextItem =  getFirstItem();
		} else {
			nextItem = getNextItemBackward(currentItem);
			if (nextItem == null)
				nextItem = getLastItem();
		}
		if (nextItem != null) {
			internalSetSelection(nextItem);
		}
	}

	private TreeItem getFirstItem() {
		TreeItem[] roots = fViewer.getTree().getItems();
		if (roots.length == 0)
			return null;
		for (TreeItem root : roots) {
			if (hasMatches(root))
				return root;
			TreeItem firstChild = getFirstChildWithMatches(roots[0]);
			if (firstChild != null)
				return firstChild;
		}
		return null;
	}

	private TreeItem getLastItem() {
		TreeItem[] roots = fViewer.getTree().getItems();
		if (roots.length == 0)
			return null;
		return getLastChildWithMatches(roots[roots.length-1]);
	}


	private TreeItem getNextItemBackward(TreeItem currentItem) {
		TreeItem previousSibling = getNextSibling(currentItem, false);
		if (previousSibling != null) {
			TreeItem lastChild = getLastChildWithMatches(previousSibling);
			if (lastChild != null)
				return lastChild;
			if (hasMatches(previousSibling))
				return previousSibling;
			return null;
		}
		TreeItem parent = currentItem.getParentItem();
		if (parent != null) {
			if (hasMatches(parent))
				return parent;
			return getNextItemBackward(parent);
		}
		return null;
	}

	private TreeItem getLastChildWithMatches(TreeItem currentItem) {
		TreeItem[] children = getChildren(currentItem);
		if (children.length == 0)
			return null;
		TreeItem recursiveChild = getLastChildWithMatches(children[children.length-1]);
		if (recursiveChild == null)
			return children[children.length-1];
		return recursiveChild;
	}

	private TreeItem getNextItemForward(TreeItem currentItem) {
		TreeItem child = getFirstChildWithMatches(currentItem);
		if (child != null)
			return child;
		TreeItem nextSibling = getNextSibling(currentItem, true);
		if (nextSibling != null) {
			if (hasMatches(nextSibling))
				return nextSibling;
			return getFirstChildWithMatches(nextSibling);
		}
		TreeItem parent = currentItem.getParentItem();
		while (parent != null) {
			nextSibling = getNextSibling(parent, true);
			if (nextSibling != null) {
				if (hasMatches(nextSibling))
					return nextSibling;
				return getFirstChildWithMatches(nextSibling);
			}
			parent = parent.getParentItem();
		}
		return null;
	}

	private TreeItem getFirstChildWithMatches(TreeItem item) {
		TreeItem[] children = getChildren(item);
		if (children.length == 0)
			return null;
		TreeItem child = children[0];

		if (hasMatches(child))
			return child;
		return getFirstChildWithMatches(child);
	}

	private TreeItem[] getChildren(TreeItem item) {
		fViewer.setExpandedState(item.getData(), true);
		return item.getItems();
	}

	private TreeItem getNextSibling(TreeItem currentItem, boolean forward) {
		TreeItem[] siblings = getSiblings(currentItem);
		if (siblings.length < 2)
			return null;
		int index = -1;
		for (int i = 0; i <siblings.length; i++) {
			if (siblings[i] == currentItem) {
				index = i;
				break;
			}
		}
		if (forward && index == siblings.length-1) {
			return null;
		} else if (!forward && index == 0) {
			return null;
		}
		return forward ? siblings[index+1] : siblings[index-1];
	}

	private TreeItem[] getSiblings(TreeItem currentItem) {
		Tree tree = fViewer.getTree();
		TreeItem parentItem = currentItem.getParentItem();
		if (parentItem != null)
			return parentItem.getItems();
		return tree.getItems();
	}

	private boolean hasMatches(TreeItem item) {
		Object element = item.getData();
		if (element instanceof DeltaSession.TreeNode) {
			TreeNode node = (TreeNode) element;
			return node.getData() != null && !node.hasChildren();
		}
		return element != null;
	}

	private TreeItem getCurrentItem(boolean forward) {
		Tree tree = fViewer.getTree();
		TreeItem[] selection = tree.getSelection();
		if (selection.length == 0) {
			selection = tree.getItems();
		}

		TreeItem nextItem = null;
		if (selection.length > 0) {
			nextItem = forward ? selection[0] : selection[selection.length-1];
		}
		return nextItem;
	}


	private void internalSetSelection(TreeItem ti) {
		if (ti != null) {
			Object data = ti.getData();
			if (data != null) {
				ISelection selection = new StructuredSelection(data);
				fViewer.setSelection(selection, true);
			}
		}
	}
}
