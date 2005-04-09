/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.ui.*;


public class SchemaPropertySheet extends PropertySheetPage {
	private Action cloneAction;
	protected ISelection currentSelection;
	private IWorkbenchPart part;
	public SchemaPropertySheet() {
		makeSchemaActions();
	}
	public void disableActions() {
		cloneAction.setEnabled(false);
	}
	public void fillLocalToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Separator());
		toolBarManager.add(cloneAction);
	}
	public IPropertySheetEntry getSelectedEntry() {
		if(!currentSelection.isEmpty() && currentSelection instanceof IStructuredSelection)
			return (IPropertySheetEntry) ((IStructuredSelection)currentSelection).getFirstElement();
		return null;
	}
	protected void handleClone() {
		Object input = null;
		if (currentSelection instanceof IStructuredSelection) {
			input = ((IStructuredSelection) currentSelection).getFirstElement();
		}
		IPropertySource source = null;
		if (input instanceof IAdaptable) {
			source = (IPropertySource) ((IAdaptable) input)
					.getAdapter(IPropertySource.class);
		}
		if (source instanceof ICloneablePropertySource) {
			Object newInput = ((ICloneablePropertySource) source).doClone();
			if (newInput != null) {
				selectionChanged(part, new StructuredSelection(newInput));
			}
		}
	}
	public void makeContributions(IMenuManager menuManager,
			IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		fillLocalToolBar(toolBarManager);
	}
	protected void makeSchemaActions() {
		cloneAction = new Action(PDEUIMessages.SchemaPropertySheet_clone_label) {
			public void run() {
				handleClone();
			}
		};
		cloneAction.setImageDescriptor(PDEPluginImages.DESC_CLONE_ATT);
		cloneAction
				.setDisabledImageDescriptor(PDEPluginImages.DESC_CLONE_ATT_DISABLED);
		cloneAction.setToolTipText(PDEUIMessages.SchemaPropertySheet_clone_tooltip);
		cloneAction.setEnabled(false);
	}
	public void selectionChanged(IWorkbenchPart part, ISelection sel) {
		super.selectionChanged(part, sel);
		this.part = part;
		currentSelection = sel;
		updateActions();
	}
	protected void updateActions() {
		Object input = null;
		if (currentSelection instanceof IStructuredSelection) {
			input = ((IStructuredSelection) currentSelection).getFirstElement();
		}
		IPropertySource source = null;
		if (input instanceof IAdaptable) {
			source = (IPropertySource) ((IAdaptable) input)
					.getAdapter(IPropertySource.class);
		}
		updateActions(source);
	}
	protected void updateActions(IPropertySource source) {
		if (source instanceof ICloneablePropertySource) {
			cloneAction.setEnabled(((ICloneablePropertySource) source)
					.isCloneable());
		} else
			cloneAction.setEnabled(false);
	}
}
