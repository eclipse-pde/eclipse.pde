/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class TreeSection extends StructuredViewerSection {
	protected boolean handleDefaultButton=true;
	class PartAdapter extends TreePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void selectionChanged(IStructuredSelection selection) {
			getManagedForm().fireSelectionChanged(TreeSection.this, selection);
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
		protected void createButtons(Composite parent, FormToolkit toolkit) {
			super.createButtons(parent, toolkit);
			enableButtons();
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
	protected void enableButtons() {
	}
}
