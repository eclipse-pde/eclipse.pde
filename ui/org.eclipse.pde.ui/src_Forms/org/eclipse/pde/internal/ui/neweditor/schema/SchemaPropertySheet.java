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
package org.eclipse.pde.internal.ui.neweditor.schema;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.ui.*;
/**
 *
 */
public class SchemaPropertySheet extends PropertySheetPage {
	public static final String CLONE_LABEL = "SchemaEditor.SchemaPropertySheet.clone.label";
	public static final String CLONE_TOOLTIP = "SchemaEditor.SchemaPropertySheet.clone.tooltip";
	private Action cloneAction;
	protected ISelection currentSelection;
	private IWorkbenchPart part;
	public SchemaPropertySheet() {
		makeSchemaActions();
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		final TableTree tableTree = (TableTree) getControl();
		tableTree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				 * TableTreeItem [] items = tableTree.getSelection();
				 * IPropertySheetEntry entry = null; if (items.length >0) entry =
				 * (IPropertySheetEntry)items[0].getData();
				 * updateActions(entry);
				 */
			}
		});
	}
	public void disableActions() {
		cloneAction.setEnabled(false);
	}
	public void fillLocalToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Separator());
		toolBarManager.add(cloneAction);
	}
	public IPropertySheetEntry getSelectedEntry() {
		TableTree tableTree = (TableTree) getControl();
		TableTreeItem[] items = tableTree.getSelection();
		IPropertySheetEntry entry = null;
		if (items.length > 0)
			entry = (IPropertySheetEntry) items[0].getData();
		return entry;
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
		cloneAction = new Action(PDEPlugin.getResourceString(CLONE_LABEL)) {
			public void run() {
				handleClone();
			}
		};
		cloneAction.setImageDescriptor(PDEPluginImages.DESC_CLONE_ATT);
		cloneAction
				.setHoverImageDescriptor(PDEPluginImages.DESC_CLONE_ATT_HOVER);
		cloneAction
				.setDisabledImageDescriptor(PDEPluginImages.DESC_CLONE_ATT_DISABLED);
		cloneAction.setToolTipText(PDEPlugin.getResourceString(CLONE_TOOLTIP));
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