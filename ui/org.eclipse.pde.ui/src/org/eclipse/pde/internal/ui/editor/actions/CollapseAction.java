/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	private Object fTreeObject;

	private int fExpandToLevel;

	/**
	 * @param viewer
	 * @param tooltipText
	 * @param expandToLevel
	 * @param treeObject
	 */
	public CollapseAction(AbstractTreeViewer viewer, String tooltipText, int expandToLevel, Object treeObject) {
		super(tooltipText, IAction.AS_PUSH_BUTTON);
		fExpandToLevel = expandToLevel;
		fTreeObject = treeObject;
		initialize(viewer, tooltipText);
	}

	/**
	 * @param viewer
	 * @param tooltipText
	 */
	public CollapseAction(AbstractTreeViewer viewer, String tooltipText) {
		super(tooltipText, IAction.AS_PUSH_BUTTON);
		fExpandToLevel = 0;
		fTreeObject = null;
		initialize(viewer, tooltipText);
	}

	/**
	 * @param viewer
	 * @param tooltipText
	 */
	private void initialize(AbstractTreeViewer viewer, String tooltipText) {
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		fTreeViewer = viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
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
