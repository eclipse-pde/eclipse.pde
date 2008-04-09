/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.util.Iterator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class ProvisionerListSelectionPage extends WizardSelectionPage {

	private TableViewer fTableViewer = null;
	private Text fTextBox = null;
	private ElementList fElements = null;

	protected ProvisionerListSelectionPage(ElementList elements) {
		super(PDEUIMessages.ProvisionerListSelectionPage_pageName);
		fElements = elements;
		setTitle(PDEUIMessages.ProvisionerListSelectionPage_title);
		setDescription(PDEUIMessages.ProvisionerListSelectionPage_description);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.None);
		label.setText(PDEUIMessages.ProvisionerListSelectionPage_tableLabel);
		label.setLayoutData(new GridData());

		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		sashForm.setLayoutData(gd);

		fTableViewer = new TableViewer(sashForm, SWT.BORDER);

		fTableViewer.setLabelProvider(ListUtil.TABLE_LABEL_PROVIDER);
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setInput(fElements.getChildren());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelection();
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (isPageComplete()) {
					getWizard().getContainer().showPage(getNextPage());
				}

			}
		});

		fTextBox = new Text(sashForm, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
		fTextBox.setText(new String());
		fTextBox.setBackground(fTableViewer.getControl().getBackground());
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_PROVISIONERS_PREFERENCE_PAGE);
		Dialog.applyDialogFont(container);

	}

	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				return (IBasePluginWizard) wizardElement.createExecutableExtension();
			}
		};
	}

	protected void setDescriptionText(String text) {
		if (text == null) {
			fTextBox.setText(""); //$NON-NLS-1$
		} else {
			fTextBox.setText(text);
		}
	}

	protected void handleSelection() {
		setErrorMessage(null);
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		WizardElement currentWizardSelection = null;
		Iterator iter = selection.iterator();
		if (iter.hasNext())
			currentWizardSelection = (WizardElement) iter.next();
		if (currentWizardSelection == null) {
			setDescriptionText(""); //$NON-NLS-1$
			setSelectedNode(null);
			setPageComplete(false);
			return;
		}
		final WizardElement finalSelection = currentWizardSelection;
		setSelectedNode(createWizardNode(finalSelection));
		setDescriptionText(finalSelection.getDescription());
		setPageComplete(true);
		getContainer().updateButtons();
	}

	public IWizard getSelectedWizard() {
		IWizardNode node = getSelectedNode();
		if (node != null)
			return node.getWizard();
		return null;
	}

}
