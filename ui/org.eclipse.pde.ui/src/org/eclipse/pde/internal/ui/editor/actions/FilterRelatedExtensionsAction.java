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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ExtensionsSection;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.util.ExtensionsFilterUtil;
import org.eclipse.swt.widgets.Text;

/**
 * Set the search pattern text to all values found by attribute list {@link ExtensionsFilterUtil#RELATED_ATTRIBUTES}  
 */
public class FilterRelatedExtensionsAction extends Action {

	protected ExtensionsSection fSection;
	protected TreeViewer fExtensionTree;
	protected FormFilteredTree fFilteredTree;

	public FilterRelatedExtensionsAction(TreeViewer treeViewer, FormFilteredTree filteredTree, ExtensionsSection section) {
		setImageDescriptor(PDEPluginImages.DESC_FILTER_RELATED);
		setDisabledImageDescriptor(PDEPluginImages.DESC_FILTER_RELATED_DISABLED);
		// Extensions section attaches this action to the global find keybinding
//		String filterBinding = ((IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class)).getBestActiveBindingFormattedFor(ActionFactory.FIND.getCommandId());
		setText(PDEUIMessages.Actions_filter_relatedPluginElements /*+ ((filterBinding != null) ? "\t" + filterBinding : "")*/);
		setToolTipText(PDEUIMessages.FilterRelatedExtensionsAction_tooltip);
		fSection = section;
		fExtensionTree = treeViewer;
		fFilteredTree = filteredTree;
	}

	public void run() {
		String filterPattern = ExtensionsFilterUtil.getFilterRelatedPattern((IStructuredSelection) fExtensionTree.getSelection());
		Text filterControl = fFilteredTree.getFilterControl();
		if (filterControl != null && filterPattern.length() > 0) {
			fSection.setBypassFilterDelay(true); // force immediate job run
			filterControl.setText(filterPattern);
		}
	}

}