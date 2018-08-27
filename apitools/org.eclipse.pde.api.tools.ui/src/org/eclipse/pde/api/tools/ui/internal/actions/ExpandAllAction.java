/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;

/**
 * An {@link Action} that will expand all nodes in a given {@link TreeViewer}.
 */
public class ExpandAllAction extends Action {

	private TreeViewer fViewer;

	public ExpandAllAction(TreeViewer viewer) {
		setText(ActionMessages.ExpandAllAction_label);
		setToolTipText(ActionMessages.ExpandAllAction_tooltip);
		ImageDescriptor enabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_EXPANDALL);
		setImageDescriptor(enabledImageDescriptor);
		ImageDescriptor disabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_DLCL_EXPANDALL);
		setDisabledImageDescriptor(disabledImageDescriptor);
		fViewer = viewer;
	}

	@Override
	public void run() {
		try {
			fViewer.getControl().setRedraw(false);
			fViewer.expandAll();
		} finally {
			fViewer.getControl().setRedraw(true);
		}
	}

}
