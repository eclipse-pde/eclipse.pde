/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.Constants;

public class ConvertAutomaticManifestsWizardSettingsPage extends UserInputWizardPage {

	private static final String SECTION_NAME = "ConvertAutomaticManifestPage"; //$NON-NLS-1$
	private static final String KEY_USE_PROJECT_ROOT = "use_project_root"; //$NON-NLS-1$
	private static final String KEY_KEEP_REQUIRE_BUNDLE = "keep_require_bundle"; //$NON-NLS-1$
	private static final String KEY_KEEP_IMPORT_PACKAGE = "keep_import_package"; //$NON-NLS-1$
	private static final String KEY_KEEP_EXPORT_PACKAGE = "keep_export_package"; //$NON-NLS-1$
	private static final String KEY_KEEP_REQUIREDEXECUTIONENVIRONMENT = "keep_requiredexecutionenvironment_package"; //$NON-NLS-1$
	private final ConvertAutomaticManifestProcessor processor;

	public ConvertAutomaticManifestsWizardSettingsPage(ConvertAutomaticManifestProcessor processor) {
		super(PDEUIMessages.ConvertAutomaticManifestWizardPage_title);
		this.processor = processor;
		setTitle(PDEUIMessages.ConvertAutomaticManifestWizardPage_title);
		setDescription(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		composite.setLayout(new GridLayout(2, false));
		createGenerateOption(composite);
		createRequireBundleOption(composite);
		createImportPackageOption(composite);
		createRequiredExecutionEnvironmentOption(composite);
		createExportPackageOption(composite);
	}

	private void createExportPackageOption(Composite parent) {
		new Label(parent, SWT.NONE).setText(Constants.EXPORT_PACKAGE);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Button optionAnnotations = new Button(composite, SWT.RADIO);
		optionAnnotations.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_convert_to_annotations);
		Button optionKeep = new Button(composite, SWT.RADIO);
		optionKeep.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_keep);
		IDialogSettings settings = getSettings();
		if (settings.getBoolean(KEY_KEEP_EXPORT_PACKAGE)) {
			optionKeep.setSelection(true);
			processor.setKeepExportPackage(true);
		} else {
			optionAnnotations.setSelection(true);
		}
		optionAnnotations.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionAnnotations.getSelection()) {
				getSettings().put(KEY_KEEP_EXPORT_PACKAGE, false);
				processor.setKeepExportPackage(false);
			}
		}));
		optionKeep.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionKeep.getSelection()) {
				getSettings().put(KEY_KEEP_EXPORT_PACKAGE, true);
				processor.setKeepExportPackage(true);
			}
		}));
	}

	@SuppressWarnings("deprecation")
	private void createRequiredExecutionEnvironmentOption(Composite parent) {
		new Label(parent, SWT.NONE).setText(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Button optionInstructions = new Button(composite, SWT.RADIO);
		optionInstructions.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_to_instructions);
		Button optionKeep = new Button(composite, SWT.RADIO);
		optionKeep.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_keep);
		IDialogSettings settings = getSettings();
		if (settings.getBoolean(KEY_KEEP_REQUIREDEXECUTIONENVIRONMENT)) {
			optionKeep.setSelection(true);
			processor.setKeepRequiredExecutionEnvironment(true);
		} else {
			optionInstructions.setSelection(true);
		}
		optionInstructions.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionInstructions.getSelection()) {
				getSettings().put(KEY_KEEP_REQUIREDEXECUTIONENVIRONMENT, false);
				processor.setKeepRequiredExecutionEnvironment(false);
			}
		}));
		optionKeep.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionKeep.getSelection()) {
				getSettings().put(KEY_KEEP_REQUIREDEXECUTIONENVIRONMENT, true);
				processor.setKeepRequiredExecutionEnvironment(true);
			}
		}));
	}

	private void createImportPackageOption(Composite parent) {
		new Label(parent, SWT.NONE).setText(Constants.IMPORT_PACKAGE);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Button optionDiscard = new Button(composite, SWT.RADIO);
		optionDiscard.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_discard);
		Button optionKeep = new Button(composite, SWT.RADIO);
		optionKeep.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_keep);
		IDialogSettings settings = getSettings();
		if (settings.getBoolean(KEY_KEEP_IMPORT_PACKAGE)) {
			optionKeep.setSelection(true);
			processor.setKeepImportPackage(true);
		} else {
			optionDiscard.setSelection(true);
		}
		optionDiscard.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionDiscard.getSelection()) {
				getSettings().put(KEY_KEEP_IMPORT_PACKAGE, false);
				processor.setKeepImportPackage(false);
			}
		}));
		optionKeep.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionKeep.getSelection()) {
				getSettings().put(KEY_KEEP_IMPORT_PACKAGE, true);
				processor.setKeepImportPackage(true);
			}
		}));
	}

	private void createRequireBundleOption(Composite parent) {
		new Label(parent, SWT.NONE).setText(Constants.REQUIRE_BUNDLE);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Button optionDiscard = new Button(composite, SWT.RADIO);
		optionDiscard.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_discard);
		Button optionKeep = new Button(composite, SWT.RADIO);
		optionKeep.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_keep);
		IDialogSettings settings = getSettings();
		if (settings.getBoolean(KEY_KEEP_REQUIRE_BUNDLE)) {
			optionKeep.setSelection(true);
			processor.setKeepRequireBundle(true);
		} else {
			optionDiscard.setSelection(true);
		}
		optionDiscard.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionDiscard.getSelection()) {
				getSettings().put(KEY_KEEP_REQUIRE_BUNDLE, false);
				processor.setKeepRequireBundle(false);
			}
		}));
		optionKeep.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionKeep.getSelection()) {
				getSettings().put(KEY_KEEP_REQUIRE_BUNDLE, true);
				processor.setKeepRequireBundle(true);
			}
		}));
	}

	private void createGenerateOption(Composite parent) {
		new Label(parent, SWT.NONE).setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_manifest);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Button optionOutput = new Button(composite, SWT.RADIO);
		optionOutput.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_manifest_at_output);
		Button optionRoot = new Button(composite, SWT.RADIO);
		optionRoot.setText(PDEUIMessages.ConvertAutomaticManifestsWizardSettingsPage_manifest_at_root);
		IDialogSettings settings = getSettings();
		if (settings.getBoolean(KEY_USE_PROJECT_ROOT)) {
			optionRoot.setSelection(true);
			processor.setUseProjectRoot(true);
		} else {
			optionOutput.setSelection(true);
			processor.setUseProjectRoot(false);
		}
		optionOutput.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionOutput.getSelection()) {
				getSettings().put(KEY_USE_PROJECT_ROOT, false);
				processor.setUseProjectRoot(false);
			}
		}));
		optionRoot.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (optionRoot.getSelection()) {
				getSettings().put(KEY_USE_PROJECT_ROOT, true);
				processor.setUseProjectRoot(true);
			}
		}));

	}

	private IDialogSettings getSettings() {
		IDialogSettings settings = getDialogSettings();
		IDialogSettings section = settings.getSection(SECTION_NAME);
		if (section == null) {
			return settings.addNewSection(SECTION_NAME);
		}
		return section;
	}

}
