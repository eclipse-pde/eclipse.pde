/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.properties;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.HashMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.CompilersPreferencePage;
import org.eclipse.pde.internal.ui.preferences.PDECompilersConfigurationBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * The PDE manifest compiler options property page for plugin projects
 */
public class CompilersPropertyPage extends PropertyPage {

	/**
	 * The data map passed when showing the page
	 */
	private HashMap<?, ?> fPageData = null;

	/**
	 * The control block
	 */
	private PDECompilersConfigurationBlock fBlock = null;

	/**
	 * If project specific settings are being used or not
	 */
	private Button fProjectSpecific = null;

	/**
	 * A link to configure workspace settings
	 */
	private Link fWorkspaceLink = null;

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		Composite tcomp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		fProjectSpecific = new Button(tcomp, SWT.CHECK);
		fProjectSpecific.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));
		fProjectSpecific.setText(PDEUIMessages.CompilersPropertyPage_useprojectsettings_label);
		fProjectSpecific.addSelectionListener(widgetSelectedAdapter(e -> {
			boolean psp = fProjectSpecific.getSelection();
			fBlock.useProjectSpecificSettings(psp);
			if (fWorkspaceLink != null) {
				fWorkspaceLink.setEnabled(!psp);
			}
		}));

		if (offerLink()) {
			fWorkspaceLink = new Link(tcomp, SWT.NONE);
			fWorkspaceLink.setText(PDEUIMessages.CompilersPropertyPage_useworkspacesettings_change);
			fWorkspaceLink.addSelectionListener(widgetSelectedAdapter(e -> {
				HashMap<String, Boolean> data = new HashMap<>();
				data.put(CompilersPreferencePage.NO_LINK, Boolean.TRUE);
				SWTFactory.showPreferencePage(getShell(), "org.eclipse.pde.ui.CompilersPreferencePage", data); //$NON-NLS-1$
			}));
		}

		fBlock = new PDECompilersConfigurationBlock(getProject(), (IWorkbenchPreferenceContainer) getContainer());
		fBlock.createControl(comp);

		boolean ps = fBlock.hasProjectSpecificSettings(getProject());
		fProjectSpecific.setSelection(ps);
		fBlock.useProjectSpecificSettings(ps);
		if (fWorkspaceLink != null) {
			fWorkspaceLink.setEnabled(!ps);
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.COMPILERS_PROPERTY_PAGE);
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * @return the backing project for this property page
	 */
	private IProject getProject() {
		return getElement().getAdapter(IProject.class);
	}

	@Override
	public boolean performCancel() {
		fBlock.performCancel();
		return super.performCancel();
	}

	@Override
	public boolean performOk() {
		fBlock.performOK();
		return super.performOk();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		fBlock.performDefaults();
		super.performDefaults();
	}

	@Override
	protected void performApply() {
		fBlock.performApply();
		super.performApply();
	}

	@Override
	public void dispose() {
		fBlock.dispose();
		super.dispose();
	}

	/**
	 * @return true if the link should be shown, false otherwise
	 */
	private boolean offerLink() {
		return fPageData == null || !Boolean.TRUE.equals(fPageData.get(CompilersPreferencePage.NO_LINK));
	}

	@Override
	public void applyData(Object data) {
		if (data instanceof HashMap) {
			fPageData = (HashMap<?, ?>) data;
			if (fWorkspaceLink != null) {
				fWorkspaceLink.setVisible(!Boolean.TRUE.equals(fPageData.get(CompilersPreferencePage.NO_LINK)));
			}

			if (fBlock == null)
				return;
			if (getProject() != null) {
				Boolean useProjectOptions = (Boolean) fPageData
						.get(CompilersPreferencePage.USE_PROJECT_SPECIFIC_OPTIONS);

				if (useProjectOptions != null) {
					fBlock.useProjectSpecificSettings(useProjectOptions.booleanValue());
					fProjectSpecific.setSelection(useProjectOptions.booleanValue());
				}
			}
			Object key = fPageData.get(CompilersPreferencePage.DATA_SELECT_OPTION_KEY);
			Object qualifier = fPageData.get(CompilersPreferencePage.DATA_SELECT_OPTION_QUALIFIER);
			if (key instanceof String && qualifier instanceof String) {
				fBlock.selectOption((String) key, (String) qualifier);
			}

		}
	}
}
