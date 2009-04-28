/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class TargetDefinitionWizardPage extends PDEWizardNewFileCreationPage {

	protected static final int USE_EMPTY = 0;
	protected static final int USE_DEFAULT = 1;
	protected static final int USE_CURRENT_TP = 2;
	protected static final int USE_EXISTING_TARGET = 3;

	private Button fEmptyButton;
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Button fExistingTargetButton;
	private Combo fTargets;
	private String[] fTargetIds;

	private static String EXTENSION = "target"; //$NON-NLS-1$

	public TargetDefinitionWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
		// Force the file extension to be 'target'
		setFileExtension(EXTENSION);
	}

	public void createControl(Composite parent) {
		super.createControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_DEFINITION_PAGE);
	}

	protected void createAdvancedControls(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp, PDEUIMessages.TargetCreationPage_0, 2);

		fEmptyButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_1, 2);
		fDefaultButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_2, 2);
		fCurrentTPButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_3, 2);
		fExistingTargetButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_4, 1);
		fExistingTargetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fExistingTargetButton.getSelection();
				fTargets.setEnabled(enabled);
			}
		});

		fEmptyButton.setSelection(true);

		fTargets = SWTFactory.createCombo(comp, SWT.SINGLE | SWT.READ_ONLY, 1, GridData.BEGINNING, null);
		fTargets.setEnabled(false);
		initializeTargetCombo();

		Dialog.applyDialogFont(comp);
		setControl(comp);

	}

	protected void initializeTargetCombo() {
		IConfigurationElement[] elements = PDECore.getDefault().getTargetProfileManager().getSortedTargets();
		fTargetIds = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			String name = elements[i].getAttribute("name"); //$NON-NLS-1$
			if (fTargets.indexOf(name) == -1)
				fTargets.add(name);
			fTargetIds[i] = elements[i].getAttribute("id"); //$NON-NLS-1$
		}
		if (elements.length > 0)
			fTargets.select(0);
	}

	protected int getInitializationOption() {
		if (fEmptyButton.getSelection())
			return USE_EMPTY;
		if (fDefaultButton.getSelection())
			return USE_DEFAULT;
		else if (fCurrentTPButton.getSelection())
			return USE_CURRENT_TP;
		return USE_EXISTING_TARGET;
	}

	protected String getTargetId() {
		return fTargetIds[fTargets.getSelectionIndex()];
	}

}
