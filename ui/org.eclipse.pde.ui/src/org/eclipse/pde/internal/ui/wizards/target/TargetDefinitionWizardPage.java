/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class TargetDefinitionWizardPage extends PDEWizardNewFileCreationPage {

	protected static final int USE_EMPTY = 0;
	protected static final int USE_DEFAULT = 1;
	protected static final int USE_CURRENT_TP = 2;
	protected static final int USE_EXISTING_TARGET = 3;

	private Button fEmptyButton;
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Combo fTargets;
	private String[] fTargetIds;

	private static final String EXTENSION = "target"; //$NON-NLS-1$

	public TargetDefinitionWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
		// Force the file extension to be 'target'
		setFileExtension(EXTENSION);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_DEFINITION_PAGE);
	}

	@Override
	protected void createAdvancedControls(Composite parent) {
		ScrolledComposite scrolled = SWTFactory.createScrolledComposite(parent, 1, 1, 5, 5);
		Composite comp = SWTFactory.createComposite(scrolled, 2, 1, GridData.FILL_BOTH);
		scrolled.setContent(comp);
		SWTFactory.createLabel(comp, PDEUIMessages.TargetCreationPage_0, 2);

		fEmptyButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_1, 2);
		fDefaultButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_2, 2);
		fCurrentTPButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_3, 2);
		Button fExistingTargetButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_4, 1);
		fExistingTargetButton.addSelectionListener(widgetSelectedAdapter(e -> {
			boolean enabled = fExistingTargetButton.getSelection();
			fTargets.setEnabled(enabled);
		}));

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
