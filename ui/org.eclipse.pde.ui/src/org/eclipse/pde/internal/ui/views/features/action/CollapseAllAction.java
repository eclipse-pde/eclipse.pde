/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class CollapseAllAction extends Action {

	private final TreeViewer fViewer;

	public CollapseAllAction(TreeViewer viewer) {
		fViewer = viewer;

		setDescription(PDEUIMessages.FeaturesView_CollapseAllAction_description);
		setToolTipText(PDEUIMessages.FeaturesView_CollapseAllAction_tooltip);
		setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
	}

	@Override
	public void run() {
		fViewer.collapseAll();
	}

}