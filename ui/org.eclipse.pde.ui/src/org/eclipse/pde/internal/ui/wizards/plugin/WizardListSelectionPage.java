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
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public class WizardListSelectionPage extends BaseWizardSelectionPage
		implements ISelectionChangedListener, IExecutableExtension {
	protected TableViewer wizardSelectionViewer;
	private ElementList wizardElements;
	private WizardSelectedAction doubleClickAction = new WizardSelectedAction();
	private ContentPage fContentPage;
	private Button fUseTemplate;
	
	private class WizardSelectedAction extends Action {
		public WizardSelectedAction() {
			super("wizardSelection");
		}
		public void run() {
			selectionChanged(new SelectionChangedEvent(wizardSelectionViewer,
					wizardSelectionViewer.getSelection()));
			advanceToNextPage();
		}
	}
	public WizardListSelectionPage(ElementList wizardElements, ContentPage page, String message) {
		super("ListSelection", message);
		this.wizardElements = wizardElements;
		fContentPage = page;
		setTitle(PDEPlugin.getResourceString("WizardListSelectionPage.title"));
		setDescription(PDEPlugin.getResourceString("WizardListSelectionPage.desc"));
	}
	public void advanceToNextPage() {
		getContainer().showPage(getNextPage());
	}
	public ElementList getWizardElements() {
		return wizardElements;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fUseTemplate = new Button(container, SWT.CHECK);
		fUseTemplate.setText(PDEPlugin.getResourceString("WizardListSelectionPage.label"));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fUseTemplate.setLayoutData(gd);
		fUseTemplate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizardSelectionViewer.getControl().setEnabled(fUseTemplate.getSelection());
				getContainer().updateButtons();
			}
		});
		
		Label label = new Label(container, SWT.NONE);
		label.setText(getLabel());
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		wizardSelectionViewer = new TableViewer(createTable(container, SWT.BORDER));
		wizardSelectionViewer.setContentProvider(new ListContentProvider());
		wizardSelectionViewer.setLabelProvider(ListUtil.TABLE_LABEL_PROVIDER);
		wizardSelectionViewer.setSorter(ListUtil.NAME_SORTER);
		wizardSelectionViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
		wizardSelectionViewer.addSelectionChangedListener(this);
		wizardSelectionViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		wizardSelectionViewer.getControl().setEnabled(false);
		
		createDescriptionIn(container);
		wizardSelectionViewer.setInput(wizardElements);
		Dialog.applyDialogFont(container);
		setControl(container);
	}
	
	private Table createTable(Composite parent, int style) {
		Table table = new Table(parent, style);
		new TableColumn(table, SWT.NONE);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnPixelData(200));
		table.setLayout(layout);
		return table;
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		setErrorMessage(null);
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		WizardElement currentWizardSelection = null;
		Iterator iter = selection.iterator();
		if (iter.hasNext())
			currentWizardSelection = (WizardElement) iter.next();
		if (currentWizardSelection == null) {
			setDescriptionText("");
			setSelectedNode(null);
			return;
		}
		final WizardElement finalSelection = currentWizardSelection;
		setSelectedNode(createWizardNode(finalSelection));
		setDescriptionText((String) finalSelection.getDescription());
		getContainer().updateButtons();
	}
	
	public IWizardPage getNextPage(boolean shouldCreate) {
		if (!shouldCreate)
			return super.getNextPage();
		IWizardNode selectedNode = getSelectedNode();
		selectedNode.dispose();
		IWizard wizard = selectedNode.getWizard();
		if (wizard == null) {
			super.setSelectedNode(null);
			return null;
		}
		if (shouldCreate)
			// Allow the wizard to create its pages
			wizard.addPages();
		return wizard.getStartingPage();
	}
	
	protected void focusAndSelectFirst() {
		Table table = wizardSelectionViewer.getTable();
		table.setFocus();
		TableItem[] items = table.getItems();
		if (items.length > 0) {
			TableItem first = items[0];
			Object obj = first.getData();
			wizardSelectionViewer.setSelection(new StructuredSelection(obj));
		}
	}

	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IPluginContentWizard wizard =
					(IPluginContentWizard) wizardElement.createExecutableExtension();
				wizard.init(fContentPage.getData());
				return wizard;
			}
		};
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	}
	
	public IPluginContentWizard getSelectedWizard() {
		if (fUseTemplate.getSelection()) {
			IWizardNode node = getSelectedNode();
			if (node != null)
				return (IPluginContentWizard)node.getWizard();
		}
		return null;
	}
	
	public boolean isPageComplete() {
		return !fUseTemplate.getSelection() || (fUseTemplate.getSelection() && getSelectedNode() != null);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardSelectionPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		IStructuredSelection ssel = (IStructuredSelection)wizardSelectionViewer.getSelection();
		return fUseTemplate.getSelection() && ssel != null && !ssel.isEmpty();
	}
}
