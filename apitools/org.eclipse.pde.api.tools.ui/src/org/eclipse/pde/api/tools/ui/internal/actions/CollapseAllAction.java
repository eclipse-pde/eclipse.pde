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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * An {@link Action} that will collapse all nodes in a given {@link TreeViewer}.
 */
public class CollapseAllAction extends Action {

	private final TreeViewer fViewer;

	public CollapseAllAction(TreeViewer viewer) {
		setText(ActionMessages.CollapseAllAction_label);
		setToolTipText(ActionMessages.CollapseAllAction_tooltip);
		ImageDescriptor enabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL);
		setImageDescriptor(enabledImageDescriptor);
		ImageDescriptor disabledImageDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL_DISABLED);
		setDisabledImageDescriptor(disabledImageDescriptor);
		fViewer = viewer;
	}

	@Override
	public void run() {
		try {
			fViewer.getControl().setRedraw(false);
			fViewer.collapseAll();
		} finally {
			fViewer.getControl().setRedraw(true);
		}
	}

}
