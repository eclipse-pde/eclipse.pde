/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class ExpandAllAction extends Action {
	private AbstractTreeViewer fTreeViewer;

	public ExpandAllAction(AbstractTreeViewer viewer, String tooltipText) {
		super(tooltipText, IAction.AS_PUSH_BUTTON);
		initialize(viewer, tooltipText);
	}

	private void initialize(AbstractTreeViewer viewer, String tooltipText) {
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_EXPAND_ALL);
		fTreeViewer = viewer;
	}

	@Override
	public void run() {

		if (fTreeViewer == null) {
			return;
		}
		fTreeViewer.expandAll();
	}

}
