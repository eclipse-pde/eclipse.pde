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
package org.eclipse.pde.internal.ui.editor.actions;

import java.util.Iterator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionNode;
import org.eclipse.pde.internal.core.text.plugin.PluginParentNode;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.search.ExtensionsPatternFilter;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Expands and collapses selected tree nodes in the extension elements tree viewer upon their expand state.
 */
public class ToggleExpandStateAction extends Action {

	public static int NEEDS_EXPAND = 1;
	public static int NEEDS_COLLAPSE = 2;

	protected TreeViewer fExtensionTree;
	protected FormFilteredTree fFilteredTree;

	public ToggleExpandStateAction(FormFilteredTree filteredTree, TreeViewer treeViewer) {
		setImageDescriptor(PDEPluginImages.DESC_TOGGLE_EXPAND_STATE);
		setDisabledImageDescriptor(PDEPluginImages.DESC_TOGGLE_EXPAND_STATE_DISABLED);
		setText(PDEUIMessages.ExtensionsPage_toggleExpandState);
		fExtensionTree = treeViewer;
		fFilteredTree = filteredTree;
	}

	public void run() {
		StructuredSelection selection = (StructuredSelection) fExtensionTree.getSelection();
		if (fExtensionTree.getTree().getSelectionCount() > 0) {
			TreeItem[] items = fExtensionTree.getTree().getSelection();
			try {
				fFilteredTree.setRedraw(false);
				int state = getStateChangeRequired(items);
				toggleExpandState(state, selection);
			} finally {
				fFilteredTree.setRedraw(true);
				fExtensionTree.refresh();
			}
		}
	}

	public void toggleExpandState(int state, StructuredSelection selection) {
		TreeItem[] items = fExtensionTree.getTree().getSelection();
		if (state == NEEDS_EXPAND) { // expand sub tree
			traverseChildrenAndSetExpanded(items); // load non-expanded children
			fExtensionTree.refresh();
			expandChildrenElements(selection.toArray(), true);
			fExtensionTree.setSelection(selection, false);
		} else { // collapse sub tree
			for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				fExtensionTree.setExpandedState(iterator.next(), false);
			}
		}
	}

	public int getStateChangeRequired(TreeItem[] selection) {
		return (traverseStateChangeRequired(selection)) ? NEEDS_EXPAND : NEEDS_COLLAPSE;
	}

	/**
	 * @param items items to be traversed
	 * @return <code>true</code> if at least one of the tree items could be expanded but is not. Otherwise <code>false</code> is returned.
	 */
	protected boolean traverseStateChangeRequired(TreeItem[] items) {
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				TreeItem treeItem = items[i];
				TreeItem[] children = treeItem.getItems();
				if (children.length > 0) {
					if (treeItem.getExpanded()) {
						if (traverseStateChangeRequired(children)) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Expands subtrees of given items. Items of type <code>PluginExtensionNode</code> that have multiple children to expand
	 * will only be expanded to the that level. Further expanding is required to reveal the whole subtree. This is for reasons of
	 * convenience.
	 * 
	 * @param items tree items to be expand with their children
	 */
	private void traverseChildrenAndSetExpanded(TreeItem[] items) {
		for (int i = 0; i < items.length; i++) {
			TreeItem treeItem = items[i];
			TreeItem[] children = treeItem.getItems();
			int extensionsChildCount = getExtensionsChildCount((IPluginParent) treeItem.getData());
			boolean furtherExpanding = !(extensionsChildCount > 1 && (!treeItem.getExpanded()));
			treeItem.setExpanded(furtherExpanding);
			if (furtherExpanding) {
				traverseChildrenAndSetExpanded(children);
			}
		}
	}

	private int getExtensionsChildCount(IPluginParent leafData) {
		int extensionsChildCount = 0;
		if (leafData != null && leafData instanceof PluginExtensionNode) {
			if (!fFilteredTree.isFiltered()) {
				return leafData.getChildCount();
			}
			ExtensionsPatternFilter filter = (ExtensionsPatternFilter) fFilteredTree.getPatternFilter();
			for (int j = 0; j < leafData.getChildCount(); j++) {
				if (filter.containsElement(leafData.getChildren()[j])) {
					extensionsChildCount++;
				}
			}
		}
		return extensionsChildCount;
	}

	/**
	 * @param children list of elements to be expand with their children
	 */
	private void expandChildrenElements(Object[] children, boolean fullExpand) {
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof PluginParentNode) {
				PluginParentNode node = (PluginParentNode) child;
				if (node.getChildCount() > 0 && fullExpand) {
					boolean furtherExpanding = !(node instanceof PluginExtensionNode && !fExtensionTree.getExpandedState(node));
					expandChildrenElements(node.getChildren(), furtherExpanding);
				} else {
					fExtensionTree.expandToLevel(node, 0);
				}
			}
		}
	}

	/**
	 * Determines whether the selected leafs are expandable
	 * 
	 * @param selection selection to test each item with
	 * @return whether the selection can be expanded
	 */
	public static boolean isExpandable(IStructuredSelection selection) {
		boolean isExpandable = false;
		if (selection != null) {
			if (!selection.isEmpty()) {
				for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
					Object element = iterator.next();
					if (element instanceof PluginParentNode) {
						PluginParentNode node = (PluginParentNode) element;
						if (node.getChildCount() > 0) {
							isExpandable = true;
							break;
						}
					}
				}
			}
		}
		return isExpandable;
	}

}