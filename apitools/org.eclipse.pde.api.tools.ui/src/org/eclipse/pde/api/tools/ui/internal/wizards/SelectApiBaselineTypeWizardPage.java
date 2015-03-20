/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class SelectApiBaselineTypeWizardPage extends WizardPage {

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
		MouseListener mouseClickListener = new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				update();
				getContainer().showPage(getNextPage());
			}
		};
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
			next = new TargetBasedApiBaselineWizardPage(null);
		} else if (locationIsDirectory.getSelection()) {
			next = new DirectoryBasedApiBaselineWizardPage(null);
		}
		((Wizard) getWizard()).addPage(next);
		return next;
	}

	private void update() {
		setPageComplete(canFlipToNextPage());
	}
}
