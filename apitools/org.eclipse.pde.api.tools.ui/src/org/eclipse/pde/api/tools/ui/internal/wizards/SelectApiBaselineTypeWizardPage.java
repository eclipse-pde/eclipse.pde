/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class SelectApiBaselineTypeWizardPage extends WizardPage {
	private TargetBasedApiBaselineWizardPage targetPage;
	private DirectoryBasedApiBaselineWizardPage directoryPage;

	protected SelectApiBaselineTypeWizardPage() {
		super(WizardMessages.ApiProfileWizardPage_1);
		setTitle(WizardMessages.ApiProfileWizardPage_1);
		setMessage(WizardMessages.ApiProfileWizardPage_3);
		setImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_WIZBAN_PROFILE));
	}

	private Button locationIsDirectory;
	private Button locationIsTarget;

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		setControl(comp);

		SWTFactory.createWrapLabel(comp, WizardMessages.SelectApiBaselineTypeWizardPage_select_baseline_source, 1);
		locationIsDirectory = SWTFactory.createRadioButton(comp, WizardMessages.SelectApiBaselineTypeWizardPage_source_installation_directory);
		locationIsTarget = SWTFactory.createRadioButton(comp, WizardMessages.SelectApiBaselineTypeWizardPage_source_target_platform);
		locationIsDirectory.setSelection(true);

		SelectionListener typeListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				update();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
		MouseListener mouseClickListener = MouseListener.mouseDoubleClickAdapter(e -> {
			update();
			getContainer().showPage(getNextPage());
		});
		locationIsTarget.addSelectionListener(typeListener);
		locationIsTarget.addMouseListener(mouseClickListener);
		locationIsDirectory.addSelectionListener(typeListener);
		locationIsDirectory.addMouseListener(mouseClickListener);

		update();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
		Dialog.applyDialogFont(comp);
	}

	@Override
	public boolean canFlipToNextPage() {
		return locationIsTarget.getSelection() || locationIsDirectory.getSelection();
	}

	@Override
	public IWizardPage getNextPage() {
		IWizardPage next = null;
		if (locationIsTarget.getSelection()) {
			if (targetPage == null) {
				targetPage = new TargetBasedApiBaselineWizardPage(null);
				((Wizard) getWizard()).addPage(targetPage);
			}
			next = targetPage;
		} else if (locationIsDirectory.getSelection()) {
			if (directoryPage == null) {
				directoryPage = new DirectoryBasedApiBaselineWizardPage(null);
				((Wizard) getWizard()).addPage(directoryPage);
			}
			next = directoryPage;
		}
		return next;
	}

	private void update() {
		setPageComplete(canFlipToNextPage());
	}
}
