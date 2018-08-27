/*******************************************************************************
 * Copyright (c) 2014, 2015 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConvertPreferencesWizardPage extends WizardPage {
	private String fPreferencesFilePath;
	private String fPluginCustomizeFilePath;
	private boolean fOverwrite;
	private Combo fPreferenceCombo;
	private Combo fPluginCustomizeCombo;
	private Button fOverwriteButton;
	private Button fMergeButton;

	public ConvertPreferencesWizardPage(String pluginCustomizationFilePath, String preferencesFilePath, boolean overwrite) {
		super("ConvertPreferencesWizardPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.ConvertPreferencesWizardPage_title);
		setDescription(PDEUIMessages.ConvertPreferencesWizardPage_description);
		this.fPreferencesFilePath = preferencesFilePath;
		this.fPluginCustomizeFilePath = pluginCustomizationFilePath;
		this.fOverwrite = overwrite;
	}

	@Override
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		Group group = new Group(container, SWT.NONE);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.ConvertPreferencesWizardPage_sourceFileGroup);

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ConvertPreferencesWizardPage_source_file);

		fPreferenceCombo = new Combo(group, SWT.BORDER);
		BidiUtils.applyBidiProcessing(fPreferenceCombo, StructuredTextTypeHandlerFactory.FILE);
		fPreferenceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button browse = new Button(group, SWT.PUSH);
		browse.setText(PDEUIMessages.ConvertPreferencesWizardPage_source_browse);
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(widgetSelectedAdapter(e -> handleBrowsePreferences()));
		SWTUtil.setButtonDimensionHint(browse);

		File prefs = getPreferencesFile();
		if (prefs != null) {
			fPreferenceCombo.add(fPreferencesFilePath, 0);
			fPreferenceCombo.setText(fPreferencesFilePath);
			if (!prefs.exists()) {
				this.setErrorMessage(NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFileNotFound, prefs.getAbsolutePath()));
			}
		}
		fPreferenceCombo.addModifyListener(event -> pageChanged(true));

		group = new Group(container, SWT.NONE);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.ConvertPreferencesWizardPage_targetFileGroup);

		label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ConvertPreferencesWizardPage_target_file);

		fPluginCustomizeCombo = new Combo(group, SWT.BORDER);
		BidiUtils.applyBidiProcessing(fPluginCustomizeCombo, StructuredTextTypeHandlerFactory.FILE);
		fPluginCustomizeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IFile customization = getCustomizationFile(fPluginCustomizeFilePath);
		if (customization != null && customization.exists()) {
			fPluginCustomizeCombo.add(fPluginCustomizeFilePath, 0);
			fPluginCustomizeCombo.setText(fPluginCustomizeFilePath);
		}

		fPluginCustomizeCombo.addModifyListener(event -> pageChanged(true));

		browse = new Button(group, SWT.PUSH);
		browse.setText(PDEUIMessages.ConvertPreferencesWizardPage_target_browse);
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(widgetSelectedAdapter(e -> handleBrowsePluginCustomization()));
		SWTUtil.setButtonDimensionHint(browse);
		group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.ConvertPreferencesWizardPage_options);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fMergeButton = new Button(group, SWT.RADIO);
		fMergeButton.setText(PDEUIMessages.ConvertPreferencesWizardPage_merge);
		fOverwriteButton = new Button(group, SWT.RADIO);
		fOverwriteButton.setText(PDEUIMessages.ConvertPreferencesWizardPage_overwrite);

		fOverwriteButton.setSelection(fOverwrite);
		fMergeButton.setSelection(!fOverwrite);

		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.CONVERT_PREFERENCES_WIZARD);

		Dialog.applyDialogFont(container);
		pageChanged(false);
	}

	private void handleBrowsePluginCustomization() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(this.getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ConvertPreferencesWizardPage_customFileTitle);
		dialog.setMessage(PDEUIMessages.ConvertPreferencesWizardPage_customFileMessage);
		dialog.addFilter(new FileExtensionFilter("ini")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		IFile ini = getPluginCustomizeFile();
		if (ini != null)
			dialog.setInitialSelection(ini);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			String value = file.getFullPath().toString();
			if (fPluginCustomizeCombo.indexOf(value) == -1)
				fPluginCustomizeCombo.add(value, 0);
			fPluginCustomizeCombo.setText(value);
			fPluginCustomizeFilePath = value;
			pageChanged(true);
		}
	}

	private IFile getCustomizationFile(String path) {
		if (path == null || path.length() == 0)
			return null;

		IPath thePath = new Path(path);
		return thePath.segmentCount() < 2 ? null : PDEPlugin.getWorkspace().getRoot().getFile(new Path(path));
	}

	private File getPreferencesFile(String path) {
		if (path == null || path.length() == 0)
			return null;
		return new File(path);
	}

	private void handleBrowsePreferences() {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setText(PDEUIMessages.ConvertPreferencesWizardPage_fileTitle);
		dialog.setFilterExtensions(new String[] {"*.epf"}); //$NON-NLS-1$
		dialog.setText(PDEUIMessages.ConvertPreferencesWizardPage_fileMessage);
		dialog.setFileName(fPreferencesFilePath);
		String path = dialog.open();
		if (path != null) {
			if (fPreferenceCombo.indexOf(path) == -1)
				fPreferenceCombo.add(path, 0);
			fPreferenceCombo.setText(path);

			fPreferencesFilePath = path;
			pageChanged(true);
		}
	}

	public File getPreferencesFile() {
		return this.getPreferencesFile(fPreferencesFilePath);
	}

	public IFile getPluginCustomizeFile() {
		return this.getCustomizationFile(fPluginCustomizeFilePath);
	}

	public boolean getOverwrite() {
		if (fOverwriteButton != null && !fOverwriteButton.isDisposed()) {
			fOverwrite = fOverwriteButton.getSelection();
		}
		return this.fOverwrite;
	}

	private void pageChanged(boolean reportError) {
		File file = getPreferencesFile(fPreferenceCombo.getText());
		String error = null;
		if (file == null) {
			error = PDEUIMessages.ConvertPreferencesWizard_errorNoFileSpecified;
			fPreferenceCombo.setFocus();
		} else {
			if (!file.isFile()) {
				error = NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFileNotFound, file.getAbsolutePath());
				fPreferenceCombo.setFocus();
			}
		}
		IFile file2 = getCustomizationFile(fPluginCustomizeCombo.getText());
		if (file2 == null) {
			error = PDEUIMessages.ConvertPreferencesWizard_errorNoFileSpecified;
			fPluginCustomizeCombo.setFocus();
		} else {
			if (!file2.exists()) {
				error = NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFileNotFound, file2.getFullPath().toString());
				fPluginCustomizeCombo.setFocus();
			}
		}

		if (error != null && reportError) {
			setErrorMessage(error);
		} else {
			setErrorMessage(null);
		}
		setPageComplete(error == null);
	}
}
