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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 274368
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public abstract class ExportOptionsTab extends AbstractExportTab {

	protected static final String S_JAR_FORMAT = "exportUpdate"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE = "exportSource"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE_FORMAT = "exportSourceFormat"; //$NON-NLS-1$
	private static final String S_QUALIFIER = "qualifier"; //$NON-NLS-1$
	private static final String S_QUALIFIER_NAME = "qualifierName"; //$NON-NLS-1$
	private static final String S_ALLOW_BINARY_CYCLES = "allowBinaryCycles"; //$NON-NLS-1$
	private static final String S_USE_WORKSPACE_COMPILED_CLASSES = "useWorkspaceCompiledClasses"; //$NON-NLS-1$

	private Button fIncludeSourceButton;
	private Combo fIncludeSourceCombo;
	protected Button fJarButton;
	private Button fQualifierButton;
	private Text fQualifierText;
	private Button fAllowBinaryCycles;
	private Button fUseWSCompiledClasses;

	public ExportOptionsTab(BaseExportWizardPage page) {
		super(page);
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		addSourceOption(container);
		addJAROption(container);
		addAdditionalOptions(container);
		addQualifierOption(container);
		addAllowBinaryCyclesSection(container);
		addUseWorkspaceCompiledClassesSection(container);

		return container;
	}

	protected void addSourceOption(Composite container) {
		Composite composite = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fIncludeSourceButton = new Button(composite, SWT.CHECK);
		fIncludeSourceButton.setText(PDEUIMessages.ExportWizard_includeSource);
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		fIncludeSourceButton.setLayoutData(gd);

		fIncludeSourceCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		fIncludeSourceCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
	}

	protected void addJAROption(Composite comp) {
		fJarButton = new Button(comp, SWT.CHECK);
		fJarButton.setText(getJarButtonText());
	}

	protected void addAllowBinaryCyclesSection(Composite comp) {
		fAllowBinaryCycles = new Button(comp, SWT.CHECK);
		fAllowBinaryCycles.setText(PDEUIMessages.ExportOptionsTab_allowBinaryCycles);
	}

	protected void addUseWorkspaceCompiledClassesSection(Composite comp) {
		fUseWSCompiledClasses = new Button(comp, SWT.CHECK);
		fUseWSCompiledClasses.setText(PDEUIMessages.ExportOptionsTab_use_workspace_classfiles);
	}

	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_packageJARs;
	}

	/**
	 * Provides an opportunity for subclasses to add additional options to the
	 * composite. Subclasses also override {@link #initialize(IDialogSettings)},
	 * {@link #saveSettings(IDialogSettings)}, {@link #hookListeners()} and
	 * {@link #validate()} for these options.
	 */
	protected void addAdditionalOptions(Composite comp) {
	}

	protected void addQualifierOption(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fQualifierButton = new Button(comp, SWT.CHECK);
		fQualifierButton.setText(PDEUIMessages.AdvancedPluginExportPage_qualifier);

		fQualifierText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fQualifierText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fQualifierText.addModifyListener(e -> fPage.pageChanged());
	}

	@Override
	protected void initialize(IDialogSettings settings) {
		fIncludeSourceButton.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		fIncludeSourceCombo.setItems(PDEUIMessages.ExportWizard_generateAssociatedSourceBundles, PDEUIMessages.ExportWizard_includeSourceInBinaryBundles);
		String sourceComboValue = settings.get(S_EXPORT_SOURCE_FORMAT) != null ? settings.get(S_EXPORT_SOURCE_FORMAT) : PDEUIMessages.ExportWizard_generateAssociatedSourceBundles;
		fIncludeSourceCombo.setText(sourceComboValue);
		fIncludeSourceCombo.setEnabled(fIncludeSourceButton.getSelection());
		fJarButton.setSelection(getInitialJarButtonSelection(settings));
		fQualifierButton.setSelection(settings.getBoolean(S_QUALIFIER));
		fQualifierText.setText(getInitialQualifierText(settings));
		fQualifierText.setEnabled(fQualifierButton.getSelection());
		fAllowBinaryCycles.setSelection(getInitialAllowBinaryCyclesSelection(settings));
		fUseWSCompiledClasses.setSelection(getInitialUseWorkspaceCompiledClassesSelection(settings));
		hookListeners();
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_JAR_FORMAT, fJarButton.getSelection());
		settings.put(S_EXPORT_SOURCE, fIncludeSourceButton.getSelection());
		settings.put(S_EXPORT_SOURCE_FORMAT, fIncludeSourceCombo.getItem(fIncludeSourceCombo.getSelectionIndex()));
		settings.put(S_QUALIFIER, fQualifierButton.getSelection());
		settings.put(S_QUALIFIER_NAME, fQualifierText.getText());
		settings.put(S_ALLOW_BINARY_CYCLES, fAllowBinaryCycles.getSelection());
		settings.put(S_USE_WORKSPACE_COMPILED_CLASSES, fUseWSCompiledClasses.getSelection());
	}

	private String getInitialQualifierText(IDialogSettings settings) {
		String qualifier = settings.get(S_QUALIFIER_NAME);
		if (qualifier == null || qualifier.equals("")) { //$NON-NLS-1$
			return QualifierReplacer.getDateQualifier();
		}
		return qualifier;
	}

	protected boolean getInitialJarButtonSelection(IDialogSettings settings) {
		String selected = settings.get(S_JAR_FORMAT);
		return selected == null ? TargetPlatformHelper.getTargetVersion() >= 3.1 : Boolean.parseBoolean(selected);
	}

	protected boolean getInitialAllowBinaryCyclesSelection(IDialogSettings settings) {
		String selected = settings.get(S_ALLOW_BINARY_CYCLES);
		return selected == null ? true : Boolean.parseBoolean(selected);
	}

	protected boolean getInitialUseWorkspaceCompiledClassesSelection(IDialogSettings settings) {
		String selected = settings.get(S_USE_WORKSPACE_COMPILED_CLASSES);
		return selected == null ? false : Boolean.parseBoolean(selected);
	}

	protected void hookListeners() {
		fIncludeSourceButton.addSelectionListener(widgetSelectedAdapter(e -> fIncludeSourceCombo.setEnabled(fIncludeSourceButton.getSelection())));

		fJarButton.addSelectionListener(widgetSelectedAdapter(e -> ((BaseExportWizardPage) fPage).adjustAdvancedTabsVisibility()));
		fQualifierButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fQualifierText.setEnabled(fQualifierButton.getSelection());
			fPage.pageChanged();
		}));
	}

	protected String validate() {
		return null;
	}

	protected boolean doExportSource() {
		return fIncludeSourceButton.getSelection();
	}

	protected boolean doExportSourceBundles() {
		return PDEUIMessages.ExportWizard_generateAssociatedSourceBundles.equals(fIncludeSourceCombo.getText());
	}

	protected boolean doBinaryCycles() {
		return fAllowBinaryCycles.getSelection();
	}

	protected boolean useWorkspaceCompiledClasses() {
		return fUseWSCompiledClasses.getSelection();
	}

	protected boolean useJARFormat() {
		return fJarButton.getSelection();
	}

	protected String getQualifier() {
		if (fQualifierText.isEnabled()) {
			String qualifier = fQualifierText.getText().trim();
			if (qualifier.length() > 0) {
				return qualifier;
			}
		}
		return null;
	}

	/**
	 * Provides the destination tab the ability to disable the JAR shape
	 * and metadata generation options when the export will be installed
	 * into the current running platform.
	 *
	 * @param enabled whether to enable or disable the controls
	 */
	protected void setEnabledForInstall(boolean enabled) {
		fQualifierButton.setEnabled(enabled);
		fQualifierText.setEnabled(enabled && fQualifierButton.getSelection());
		fJarButton.setEnabled(enabled);
	}

}
