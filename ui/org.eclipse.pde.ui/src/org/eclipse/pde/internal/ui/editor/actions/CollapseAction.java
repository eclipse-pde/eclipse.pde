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
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.StructuredViewerSection;

public class CollapseAction extends Action {

	private StructuredViewerSection fSection;

	public CollapseAction(StructuredViewerSection section, String tooltipText) {
		super("collapse"); //$NON-NLS-1$
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		fSection = section;
	}

	public void run() {
		StructuredViewer viewer = fSection.getStructuredViewerPart().getViewer();
		if(viewer instanceof TreeViewer)
			((TreeViewer) viewer).collapseAll();
	}

}
