/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author
 */
public abstract class TreeSection extends StructuredViewerSection {
	protected boolean handleDefaultButton=true;
	class PartAdapter extends TreePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void selectionChanged(IStructuredSelection selection) {
			getForm().fireSelectionChanged(TreeSection.this, selection);
			TreeSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			TreeSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			TreeSection.this.buttonSelected(index);
			if (handleDefaultButton)
				button.getShell().setDefaultButton(null);
		}
	}
	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public TreeSection(PDEFormPage formPage, Composite parent, int style, String[] buttonLabels) {
		super(formPage, parent, style, buttonLabels);
	}

	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		return new PartAdapter(buttonLabels);
	}

	protected TreePart getTreePart() {
		return (TreePart) viewerPart;
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}
