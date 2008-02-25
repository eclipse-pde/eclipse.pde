/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class OrganizeManifestsWizardPage extends UserInputWizardPage implements IPreferenceConstants, IOrganizeManifestsSettings {

	private Button fRemoveUnresolved;
	private Button fCalculateUses;
	private Button fAddMissing;
	private Button fMarkInternal;
	private Text fPackageFilter;
	private Label fPackageFilterLabel;
	private Button fRemoveImport;
	private Button fOptionalImport;
	private Button fModifyDependencies;
	private Button fUnusedDependencies;
	private Button fAdditonalDependencies;
	private Button fFixIconNLSPaths;
	private Button fRemovedUnusedKeys;
	private Button fRemoveLazy;
	private Button fRemoveUselessFiles;

	private Button[] fTopLevelButtons; // used for setting page complete state

	private OrganizeManifestsProcessor fProcessor;

	private static String title = PDEUIMessages.OrganizeManifestsWizardPage_title;

	protected OrganizeManifestsWizardPage() {
		super(title);
		setTitle(title);
		setDescription(PDEUIMessages.OrganizeManifestsWizardPage_description);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fProcessor = (OrganizeManifestsProcessor) ((PDERefactor) getRefactoring()).getProcessor();

		createExportedPackagesGroup(container);
		createRequireImportGroup(container);
		createGeneralGroup(container);
		createNLSGroup(container);

		// init
		setButtonArrays();
		presetOptions();
		hookListeners();

		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.ORGANIZE_MANIFESTS);
		Dialog.applyDialogFont(container);
	}

	private void createExportedPackagesGroup(Composite container) {
		Group group = createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_exportedGroup, 1, true);

		fAddMissing = new Button(group, SWT.CHECK);
		fAddMissing.setText(PDEUIMessages.OrganizeManifestsWizardPage_addMissing);

		fMarkInternal = new Button(group, SWT.CHECK);
		fMarkInternal.setText(PDEUIMessages.OrganizeManifestsWizardPage_markInternal);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPackageFilterLabel = new Label(comp, SWT.NONE);
		fPackageFilterLabel.setText(PDEUIMessages.OrganizeManifestsWizardPage_packageFilter);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		fPackageFilterLabel.setLayoutData(gd);
		fPackageFilter = new Text(comp, SWT.BORDER);
		fPackageFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fRemoveUnresolved = new Button(group, SWT.CHECK);
		fRemoveUnresolved.setText(PDEUIMessages.OrganizeManifestsWizardPage_removeUnresolved);
		gd = new GridData();
		gd.verticalIndent = 5;
		fRemoveUnresolved.setLayoutData(gd);

		fCalculateUses = new Button(group, SWT.CHECK);
		fCalculateUses.setText(PDEUIMessages.OrganizeManifestsWizardPage_calculateUses);
		gd = new GridData();
		gd.verticalIndent = 5;
		fCalculateUses.setLayoutData(gd);
	}

	private void createRequireImportGroup(Composite container) {
		Group group = createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_dependenciesGroup, 1, true);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fModifyDependencies = new Button(comp, SWT.CHECK);
		fModifyDependencies.setText(PDEUIMessages.OrganizeManifestsWizardPage_unresolvedDependencies);

		fRemoveImport = new Button(comp, SWT.RADIO);
		fRemoveImport.setText(PDEUIMessages.OrganizeManifestsWizardPage_remove);

		fOptionalImport = new Button(comp, SWT.RADIO);
		fOptionalImport.setText(PDEUIMessages.OrganizeManifestsWizardPage_markOptional);

		fUnusedDependencies = new Button(group, SWT.CHECK);
		fUnusedDependencies.setText(PDEUIMessages.OrganizeManifestsWizardPage_removeUnused);

		fAdditonalDependencies = new Button(group, SWT.CHECK);
		fAdditonalDependencies.setText(PDEUIMessages.OrganizeManifestsWizardPage_addDependencies);
	}

	private void createGeneralGroup(Composite container) {
		Group group = createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_generalGroup, 1, true);

		fRemoveLazy = new Button(group, SWT.CHECK);
		fRemoveLazy.setText(PDEUIMessages.OrganizeManifestsWizardPage_lazyStart);

		fRemoveUselessFiles = new Button(group, SWT.CHECK);
		fRemoveUselessFiles.setText(PDEUIMessages.OrganizeManifestsWizardPage_uselessPluginFile);

	}

	private void createNLSGroup(Composite container) {
		Group group = createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_internationalizationGroup, 1, true);

		fFixIconNLSPaths = new Button(group, SWT.CHECK);
		fFixIconNLSPaths.setText(PDEUIMessages.OrganizeManifestsWizardPage_prefixNL);

		fRemovedUnusedKeys = new Button(group, SWT.CHECK);
		fRemovedUnusedKeys.setText(PDEUIMessages.OrganizeManifestsWizardPage_removeUnusedKeys);
	}

	private Group createGroup(Composite parent, String text, int span, boolean colsEqual) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(text);
		GridLayout layout = new GridLayout(span, colsEqual);
		layout.marginHeight = layout.marginWidth = 10;
		group.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(gd);
		return group;
	}

	private void presetOptions() {
		IDialogSettings settings = getDialogSettings();

		boolean selection = !settings.getBoolean(PROP_ADD_MISSING);
		fAddMissing.setSelection(selection);
		fProcessor.setAddMissing(selection);

		selection = !settings.getBoolean(PROP_MARK_INTERNAL);
		fMarkInternal.setSelection(selection);
		fProcessor.setMarkInternal(selection);

		String filter = settings.get(PROP_INTERAL_PACKAGE_FILTER);
		if (filter == null)
			filter = VALUE_DEFAULT_FILTER;
		fPackageFilter.setText(filter);
		fProcessor.setPackageFilter(filter);

		selection = !settings.getBoolean(PROP_REMOVE_UNRESOLVED_EX);
		fRemoveUnresolved.setSelection(selection);
		fProcessor.setRemoveUnresolved(selection);

		selection = settings.getBoolean(PROP_CALCULATE_USES);
		fCalculateUses.setSelection(selection);
		fProcessor.setCalculateUses(selection);

		selection = !settings.getBoolean(PROP_MODIFY_DEP);
		fModifyDependencies.setSelection(selection);
		fProcessor.setModifyDep(selection);

		selection = settings.getBoolean(PROP_RESOLVE_IMP_MARK_OPT);
		fRemoveImport.setSelection(!selection);
		fOptionalImport.setSelection(selection);
		fProcessor.setRemoveDependencies(!selection);

		selection = settings.getBoolean(PROP_UNUSED_DEPENDENCIES);
		fUnusedDependencies.setSelection(selection);
		fProcessor.setUnusedDependencies(selection);

		selection = settings.getBoolean(PROP_ADD_DEPENDENCIES);
		fAdditonalDependencies.setSelection(selection);
		fProcessor.setAddDependencies(selection);

		selection = !settings.getBoolean(PROP_REMOVE_LAZY);
		fRemoveLazy.setSelection(selection);
		fProcessor.setRemoveLazy(selection);

		selection = !settings.getBoolean(PROP_REMOVE_USELESSFILES);
		fRemoveUselessFiles.setSelection(selection);
		fProcessor.setRemoveUselessFiles(selection);

		selection = settings.getBoolean(PROP_NLS_PATH);
		fFixIconNLSPaths.setSelection(selection);
		fProcessor.setPrefixIconNL(selection);

		selection = settings.getBoolean(PROP_UNUSED_KEYS);
		fRemovedUnusedKeys.setSelection(selection);
		fProcessor.setUnusedKeys(selection);

		setEnabledStates();
		setPageComplete();
	}

	protected void performOk() {
		IDialogSettings settings = getDialogSettings();

		settings.put(PROP_ADD_MISSING, !fAddMissing.getSelection());
		settings.put(PROP_MARK_INTERNAL, !fMarkInternal.getSelection());
		settings.put(PROP_INTERAL_PACKAGE_FILTER, fPackageFilter.getText());
		settings.put(PROP_REMOVE_UNRESOLVED_EX, !fRemoveUnresolved.getSelection());
		settings.put(PROP_CALCULATE_USES, fCalculateUses.getSelection());

		settings.put(PROP_MODIFY_DEP, !fModifyDependencies.getSelection());
		settings.put(PROP_RESOLVE_IMP_MARK_OPT, fOptionalImport.getSelection());
		settings.put(PROP_UNUSED_DEPENDENCIES, fUnusedDependencies.getSelection());
		settings.put(PROP_ADD_DEPENDENCIES, fAdditonalDependencies.getSelection());

		settings.put(PROP_REMOVE_LAZY, !fRemoveLazy.getSelection());
		settings.put(PROP_REMOVE_USELESSFILES, !fRemoveUselessFiles.getSelection());

		settings.put(PROP_NLS_PATH, fFixIconNLSPaths.getSelection());
		settings.put(PROP_UNUSED_KEYS, fRemovedUnusedKeys.getSelection());
	}

	private void setEnabledStates() {
		boolean markInternal = fMarkInternal.getSelection();
		fPackageFilter.setEnabled(markInternal);
		fPackageFilter.setEditable(markInternal);
		fPackageFilterLabel.setEnabled(markInternal);

		boolean modifyDependencies = fModifyDependencies.getSelection();
		fRemoveImport.setEnabled(modifyDependencies);
		fOptionalImport.setEnabled(modifyDependencies);
	}

	private void setButtonArrays() {
		fTopLevelButtons = new Button[] {fRemoveUnresolved, fAddMissing, fModifyDependencies, fMarkInternal, fUnusedDependencies, fAdditonalDependencies, fFixIconNLSPaths, fRemovedUnusedKeys, fRemoveLazy, fRemoveUselessFiles, fCalculateUses};
	}

	private void setPageComplete() {
		boolean pageComplete = false;
		for (int i = 0; i < fTopLevelButtons.length; i++) {
			if (fTopLevelButtons[i].getSelection()) {
				pageComplete = true;
				break;
			}
		}
		setPageComplete(pageComplete);
	}

	private void hookListeners() {
		hookListener(new Button[] {fMarkInternal, fModifyDependencies}, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setEnabledStates();
				doProcessorSetting(e.getSource());
			}
		});
		hookListener(fTopLevelButtons, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete();
				doProcessorSetting(e.getSource());
			}
		});
		hookListener(new Button[] {fRemoveImport, fOptionalImport}, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doProcessorSetting(e.getSource());
			}
		});
	}

	private void doProcessorSetting(Object source) {
		if (fProcessor == null)
			return;
		if (fAddMissing.equals(source))
			fProcessor.setAddMissing(fAddMissing.getSelection());
		else if (fMarkInternal.equals(source))
			fProcessor.setMarkInternal(fMarkInternal.getSelection());
		else if (fPackageFilter.equals(source))
			fProcessor.setPackageFilter(fPackageFilter.getText());
		else if (fRemoveUnresolved.equals(source))
			fProcessor.setRemoveUnresolved(fRemoveUnresolved.getSelection());
		else if (fCalculateUses.equals(source))
			fProcessor.setCalculateUses(fCalculateUses.getSelection());
		else if (fModifyDependencies.equals(source))
			fProcessor.setModifyDep(fModifyDependencies.getSelection());
		else if (fOptionalImport.equals(source))
			fProcessor.setRemoveDependencies(!fOptionalImport.getSelection());
		else if (fRemoveImport.equals(source))
			fProcessor.setRemoveDependencies(fRemoveImport.getSelection());
		else if (fUnusedDependencies.equals(source))
			fProcessor.setUnusedDependencies(fUnusedDependencies.getSelection());
		else if (fAdditonalDependencies.equals(source))
			fProcessor.setAddDependencies(fAdditonalDependencies.getSelection());
		else if (fRemoveLazy.equals(source))
			fProcessor.setRemoveLazy(fRemoveLazy.getSelection());
		else if (fRemoveUselessFiles.equals(source))
			fProcessor.setRemoveUselessFiles(fRemoveUselessFiles.getSelection());
		else if (fFixIconNLSPaths.equals(source))
			fProcessor.setPrefixIconNL(fFixIconNLSPaths.getSelection());
		else if (fRemovedUnusedKeys.equals(source))
			fProcessor.setUnusedKeys(fRemovedUnusedKeys.getSelection());
	}

	private void hookListener(Button[] buttons, SelectionAdapter adapter) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].addSelectionListener(adapter);
		}
	}
}
