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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.*;

public class ManifestPropertySheet extends PropertySheetPage {
	public static final String GOTO_ACTION_LABEL = "ManifestEditor.ManifestPropertySheet.gotoAction.label";
	public static final String GOTO_ACTION_TOOLTIP = "ManifestEditor.ManifestPropertySheet.gotoAction.tooltip";
	protected Action gotoAction;
	protected PDEMultiPageEditor editor;
	protected IPropertySource source;
	protected ISelection currentSelection;
	protected Object currentInput;
	protected IWorkbenchPart currentPart;
	private TableTree tableTree;

public ManifestPropertySheet(PDEMultiPageEditor editor) {
	this.editor = editor;
	makeActions();
}
public void createControl(Composite parent) {
	super.createControl(parent);
	tableTree = (TableTree)super.getControl();
	tableTree.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			TableTreeItem [] items = tableTree.getSelection();
			IPropertySheetEntry entry = null;
			if (items.length >0) entry = (IPropertySheetEntry)items[0].getData();
			updateActions(entry);
		}
	});

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.setRemoveAllWhenShown(true);
	popupMenuManager.addMenuListener(listener);
	Menu menu=popupMenuManager.createContextMenu(tableTree.getTable());
	tableTree.getTable().setMenu(menu);
}
public void disableActions() {
	gotoAction.setEnabled(false);
}
protected void doFillLocalMenuBar(IMenuManager menuManager) {
	menuManager.add(new Separator());
	menuManager.add(gotoAction);
}
protected void doFillLocalToolBar(IToolBarManager toolBarManager) {
	toolBarManager.add(new Separator());
	toolBarManager.add(gotoAction);
}
public void fillContextMenu(IMenuManager manager) {
	if (gotoAction.isEnabled()) 
	   manager.add(gotoAction);
}
public void fillLocalMenuBar(IMenuManager menuManager) {
	doFillLocalMenuBar(menuManager);
}
public void fillLocalToolBar(IToolBarManager toolBarManager) {
	doFillLocalToolBar(toolBarManager);
}
public Control getControl() {
	return tableTree;
}
public IPropertySheetEntry getSelectedEntry() {
	TableTreeItem[] items = tableTree.getSelection();
	IPropertySheetEntry entry = null;
	if (items.length > 0)
		entry = (IPropertySheetEntry) items[0].getData();
	return entry;
}
protected void handleOpen() {
	IPropertySheetEntry entry = getSelectedEntry();
	if (entry == null) return;
	Object input = null;
	if (currentSelection instanceof IStructuredSelection) {
		input = ((IStructuredSelection)currentSelection).getFirstElement();
	}
	IPropertySource source = null;
	if (input instanceof IAdaptable) {
		source = (IPropertySource)((IAdaptable)input).getAdapter(IPropertySource.class);
	}
	if (source instanceof IOpenablePropertySource) {
		((IOpenablePropertySource)source).openInEditor(entry);
	}
}
protected boolean isCompatible(IPropertySource s1, IPropertySource s2) {
	if (s1 instanceof UnknownElementPropertySource &&
		!(s2 instanceof UnknownElementPropertySource)) return false;
	if (s2 instanceof UnknownElementPropertySource &&
		!(s1 instanceof UnknownElementPropertySource)) return false;
	if (s1 instanceof ExtensionPropertySource) return false;
	if (s2 instanceof ExtensionPropertySource) return false;
	return true;
}
protected boolean isEditable() {
	return ((IModel)editor.getModel()).isEditable();
}
protected void makeActions() {
	gotoAction = new Action ("goto") {
		public void run() {
			handleOpen();
		}
	};
	gotoAction.setImageDescriptor(PDEPluginImages.DESC_GOTOOBJ);
	gotoAction.setHoverImageDescriptor(PDEPluginImages.DESC_GOTOOBJ_HOVER);
	gotoAction.setDisabledImageDescriptor(PDEPluginImages.DESC_GOTOOBJ_DISABLED);
	gotoAction.setText(PDEPlugin.getResourceString(GOTO_ACTION_LABEL));
	gotoAction.setToolTipText(PDEPlugin.getResourceString(GOTO_ACTION_TOOLTIP));
	gotoAction.setEnabled(false);
}
public void makeContributions(
	IMenuManager menuManager,
	IToolBarManager toolBarManager,
	IStatusLineManager statusLineManager) {
	fillLocalToolBar(toolBarManager);
	fillLocalMenuBar(menuManager);
	super.makeContributions(new MenuManager(), new ToolBarManager(), statusLineManager);
}
protected void refreshInput() {
	super.selectionChanged(currentPart, currentSelection);
}
public void selectionChanged(IWorkbenchPart part, ISelection sel) {
	super.selectionChanged(part, sel);
	currentPart = part;
	currentSelection = sel;
	currentInput = null;
	if (currentSelection instanceof IStructuredSelection) {
		currentInput = ((IStructuredSelection)currentSelection).getFirstElement();
	}
	IPropertySource oldSource = source;
	source = null;
	if (currentInput instanceof IAdaptable) {
		source = (IPropertySource)((IAdaptable)currentInput).getAdapter(IPropertySource.class);
	}
	if (!isCompatible(oldSource, source)) {
		switchBars();
	}
	disableActions();
}
public void superMakeContributions(
	IMenuManager menuManager,
	IToolBarManager toolBarManager,
	IStatusLineManager statusLineManager) {
	super.makeContributions(menuManager, toolBarManager, statusLineManager);
}
protected void switchBars() {
}
protected void updateActions(IPropertySheetEntry entry) {
	if (source instanceof IOpenablePropertySource) {
		gotoAction.setEnabled(entry!=null && ((IOpenablePropertySource)source).isOpenable(entry));
	}
}
}
