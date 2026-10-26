/*******************************************************************************
 * Copyright (c) 2005, 2026 IBM Corporation and others.
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
 *     Christoph Läubrich - extracted from ExportOptionsTab and BaseExportWizardPage
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * Export options of the legacy, Ant based export. In addition to the common
 * options it allows to save the export as an Ant script.
 */
public class AntExportOptionsTab extends ExportOptionsTab {

	private static final String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private static final String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$

	private Button fSaveAsAntButton;
	private Combo fAntCombo;
	private Button fBrowseAnt;

	public AntExportOptionsTab(BaseExportWizardPage page) {
		super(page);
	}

	@Override
	protected void addAdditionalOptions(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fSaveAsAntButton = new Button(comp, SWT.CHECK);
		fSaveAsAntButton.setText(PDEUIMessages.ExportWizard_antCheck);
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		fSaveAsAntButton.setLayoutData(gd);

		fAntCombo = new Combo(comp, SWT.NONE);
		fAntCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseAnt = new Button(comp, SWT.PUSH);
		fBrowseAnt.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseAnt.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseAnt);
	}

	@Override
	protected void initialize(IDialogSettings settings) {
		// Initialize the widgets before super hooks the listeners, otherwise setText on the combo triggers a premature page validation
		fSaveAsAntButton.setSelection(settings.getBoolean(S_SAVE_AS_ANT));
		initializeCombo(settings, S_ANT_FILENAME, fAntCombo);
		fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
		fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
		super.initialize(settings);
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		settings.put(S_SAVE_AS_ANT, fSaveAsAntButton.getSelection());
		saveCombo(settings, S_ANT_FILENAME, fAntCombo);
	}

	@Override
	protected void hookListeners() {
		super.hookListeners();
		fSaveAsAntButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
			fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
			fPage.pageChanged();
		}));

		fBrowseAnt.addSelectionListener(widgetSelectedAdapter(e -> chooseFile(fAntCombo, new String[] { "*.xml" }))); //$NON-NLS-1$

		fAntCombo.addSelectionListener(widgetSelectedAdapter(e -> fPage.pageChanged()));

		fAntCombo.addModifyListener(e -> fPage.pageChanged());
	}

	@Override
	protected String validate() {
		if (fSaveAsAntButton.getSelection() && fAntCombo.getText().trim().length() == 0) {
			return PDEUIMessages.ExportWizard_status_noantfile;
		}
		return super.validate();
	}

	protected String validateAntCombo() {
		String path = IPath.fromOSString(fAntCombo.getText()).lastSegment();
		if ("build.xml".equals(path)) { //$NON-NLS-1$
			return PDEUIMessages.ExportOptionsTab_antReservedMessage;
		}
		return null;
	}

	protected boolean doGenerateAntFile() {
		return fSaveAsAntButton.getSelection();
	}

	protected String getAntBuildFileName() {
		return fSaveAsAntButton.getSelection() ? fAntCombo.getText() : null;
	}
}
