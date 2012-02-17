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

import java.util.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.plugin.PluginNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.search.ExtensionsPatternFilter;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Reveals all extensions when the tree is in filter mode.
 * The purpose is convenience. When the Filter Related action shows that 
 * a certain item that should have been found with the search is missing,
 * it is convenient to bring up the required extension to add the
 * missing element without loosing the focus on the search result.
 * Once all extensions are revealed this action hides them again except
 * for those extensions that received new elements. 
 * 
 * @author Sascha Becher
 */
public class ShowAllExtensionsAction extends Action {

	private static int SHOW_ALL = 0;
	private static int HIDE_UNFILTERED = 1;

	private int mode;
	private FormFilteredTree fFilteredTree;
	private IPluginModelBase fModel;
	private ExtensionsPatternFilter fPatternFilter;

	public ShowAllExtensionsAction(IBaseModel model, FormFilteredTree filteredTree, ExtensionsPatternFilter patternFilter) {
		fModel = (IPluginModelBase) model;
		fFilteredTree = filteredTree;
		fPatternFilter = patternFilter;
		mode = getRequiredChange();
		setText(mode == SHOW_ALL ? PDEUIMessages.ShowAllExtensionsAction_label : PDEUIMessages.HideUnfilteredExtensionsAction_label);
	}

	public void run() {
		if (mode == SHOW_ALL) {
			IPluginExtension[] extensions = fModel.getExtensions().getExtensions();
			try {
				ISelection selection = fFilteredTree.getViewer().getSelection();
				Object[] expanded = fFilteredTree.getViewer().getVisibleExpandedElements();
				fFilteredTree.setRedraw(false);
				for (int i = 0; i < extensions.length; i++) {
					fPatternFilter.addElement(extensions[i]);
				}
				fFilteredTree.update();
				fFilteredTree.redraw();
				fFilteredTree.getViewer().refresh();

				TreeItem[] treeItems = fFilteredTree.getViewer().getTree().getItems();
				for (int i = 0; i < treeItems.length; i++) {
					TreeItem treeItem = treeItems[i];
					if (treeItem != null && !treeItem.getExpanded()) {
						treeItem.setExpanded(true);
					}
				}
				fFilteredTree.getViewer().refresh();
				fFilteredTree.getViewer().setExpandedElements(expanded);
				fFilteredTree.getViewer().setSelection(selection);
			} finally {
				fFilteredTree.setRedraw(true);
			}
		} else if (mode == HIDE_UNFILTERED) {
			List unfiltered = getUnfilteredExtensions(fPatternFilter.getMatchingLeafs());
			for (Iterator iterator = unfiltered.iterator(); iterator.hasNext();) {
				fPatternFilter.removeElement(iterator.next());
			}
		}
		fFilteredTree.getViewer().refresh();
	}

	public int getRequiredChange() {
		boolean visible = true;
		IPluginExtension[] extensions = fModel.getExtensions().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension iPluginExtension = extensions[i];
			visible &= fPatternFilter.containsElement(iPluginExtension);
		}
		if (visible) {
			return HIDE_UNFILTERED;
		}
		return SHOW_ALL;
	}

	private List getUnfilteredExtensions(Collection matchingLeafs) {
		List unfilteredExtensions = new ArrayList();
		try {
			fFilteredTree.getViewer().setExpandPreCheckFilters(true);
			IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IPluginExtension iPluginExtension = extensions[i];
				boolean found = false;
				for (Iterator it = matchingLeafs.iterator(); it.hasNext();) {
					IPluginObject element = ((IPluginObject) it.next());
					while (element.getParent() != null && !(element.getParent() instanceof PluginNode)) {
						element = element.getParent();
					}
					if (element.equals(iPluginExtension) || fFilteredTree.getViewer().isExpandable(iPluginExtension)) {
						found = true;
						break;
					}
				}
				if (!found) {
					unfilteredExtensions.add(iPluginExtension);
				}
			}
		} finally {
			fFilteredTree.getViewer().setExpandPreCheckFilters(false);
		}
		return unfilteredExtensions;
	}

}