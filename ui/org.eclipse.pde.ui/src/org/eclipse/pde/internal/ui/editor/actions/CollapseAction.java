/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class CollapseAction extends Action {

	private AbstractTreeViewer fViewer;

	public CollapseAction(AbstractTreeViewer viewer, String tooltipText) {
		super("collapse"); //$NON-NLS-1$
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		fViewer = viewer;
	}

	public void run() {
		fViewer.collapseAll();
	}

}
