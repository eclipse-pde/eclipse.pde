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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.StructuredViewerSection;

public class SortAction extends Action {

	private boolean fSorted = false;
	private StructuredViewerSection fSection;
	private ViewerSorter fSorter;

	public SortAction(StructuredViewerSection section, String tooltipText) {
		super(tooltipText, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_ALPHAB_SORT_CO);
		fSorted = section.getStructuredViewerPart().getViewer().getSorter() == null ? false : true;
		setChecked(fSorted);
		fSection = section;
	}
	
	public SortAction(StructuredViewerSection section, String tooltipText, ViewerSorter sorter) {
		super(tooltipText, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		setToolTipText(tooltipText);
		setImageDescriptor(PDEPluginImages.DESC_ALPHAB_SORT_CO);
		fSorted = section.getStructuredViewerPart().getViewer().getSorter() == null ? false : true;
		setChecked(fSorted);
		fSection = section;
		fSorter = sorter;
	}

	public void run() {
		ViewerSorter viewerSorter;
		if(fSorted) {
			viewerSorter = null;
		} else {
			viewerSorter = (fSorter == null ? new ViewerSorter() : fSorter);
		}
		fSection.getStructuredViewerPart().getViewer().setSorter(viewerSorter);
		fSorted = !fSorted;
	}

}
