/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

public class CollapseAction extends Action {

	private AbstractTreeViewer fTreeViewer;

	private final Object fTreeObject;

	private final int fExpandToLevel;

	public CollapseAction(AbstractTreeViewer viewer, String tooltipText, int expandToLevel, Object treeObject) {
		super(tooltipText, IAction.AS_PUSH_BUTTON);
		fExpandToLevel = expandToLevel;
		fTreeObject = treeObject;
		initialize(viewer, tooltipText);
	}

	public CollapseAction(AbstractTreeViewer viewer, String tooltipText) {
		super(tooltipText, IAction.AS_PUSH_BUTTON);
		fExpandToLevel = 0;
		fTreeObject = null;
		initialize(viewer, tooltipText);
	}

	private void initialize(AbstractTreeViewer viewer, String tooltipText) {
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		fTreeViewer = viewer;
	}

	@Override
	public void run() {

		if (fTreeViewer == null) {
			return;
		} else if ((fTreeObject != null) && (fExpandToLevel > 0)) {
			// Redraw modification needed to avoid flicker
			// Collapsing to a specific level does not work
			fTreeViewer.getControl().setRedraw(false);
			fTreeViewer.collapseAll();
			fTreeViewer.expandToLevel(fTreeObject, 1);
			fTreeViewer.getControl().setRedraw(true);
		} else {
			fTreeViewer.collapseAll();
		}

	}

}
