/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513, 232706
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.Iterator;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class OrganizeManifestsWizardPage extends UserInputWizardPage implements ILaunchingPreferenceConstants, IOrganizeManifestsSettings {

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
	private Set fCustomProjects;

	private static String title = PDEUIMessages.OrganizeManifestsWizardPage_title;

	protected OrganizeManifestsWizardPage(java.util.Set/*<IProject>*/customProjects) {
		super(title);
		setTitle(title);
		setDescription(PDEUIMessages.OrganizeManifestsWizardPage_description);
		fCustomProjects = customProjects;
	}

	public void createControl(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);

		fProcessor = (OrganizeManifestsProcessor) ((PDERefactor) getRefactoring()).getProcessor();

		if (!fCustomProjects.isEmpty()) {
			createCustomBuildWarning(container);
		}
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

	private void createCustomBuildWarning(Composite container) {
		Composite parent = SWTFactory.createComposite(container, 2, 1, GridData.FILL_HORIZONTAL);

		Label image = new Label(parent, SWT.NONE);
		image.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		image.setLayoutData(gd);

		String message;
		if (fCustomProjects.size() == 1) {
			message = NLS.bind(PDEUIMessages.OrganizeManifestsWizardPage_ProjectsUsingCustomBuildWarning, ((IProject) fCustomProjects.iterator().next()).getName());
		} else {
			StringBuffer buf = new StringBuffer();
			for (Iterator iterator = fCustomProjects.iterator(); iterator.hasNext();) {
				IProject project = (IProject) iterator.next();
				buf.append(project.getName());
				if (iterator.hasNext()) {
					buf.append(',').append(' ');
				}
			}
			message = NLS.bind(PDEUIMessages.OrganizeManifestsWizardPage_ProjectsUsingCustomBuildWarningPlural, buf.toString());
		}

		// Using a link as a wrap label appear to force the wizard to max vertical space
		Link link = new Link(parent, SWT.WRAP);
		link.setText(message);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		link.setLayoutData(gd);
	}

	private void createExportedPackagesGroup(Composite container) {
		Group group = SWTFactory.createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_exportedGroup, 1, 1, GridData.FILL_HORIZONTAL);

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
		Group group = SWTFactory.createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_dependenciesGroup, 1, 1, GridData.FILL_HORIZONTAL);

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
		Group group = SWTFactory.createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_generalGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fRemoveLazy = new Button(group, SWT.CHECK);
		fRemoveLazy.setText(PDEUIMessages.OrganizeManifestsWizardPage_lazyStart);

		fRemoveUselessFiles = new Button(group, SWT.CHECK);
		fRemoveUselessFiles.setText(PDEUIMessages.OrganizeManifestsWizardPage_uselessPluginFile);

	}

	private void createNLSGroup(Composite container) {
		Group group = SWTFactory.createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_internationalizationGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fFixIconNLSPaths = new Button(group, SWT.CHECK);
		fFixIconNLSPaths.setText(PDEUIMessages.OrganizeManifestsWizardPage_prefixNL);

		fRemovedUnusedKeys = new Button(group, SWT.CHECK);
		fRemovedUnusedKeys.setText(PDEUIMessages.OrganizeManifestsWizardPage_removeUnusedKeys);
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
		hookSelectionListener(new Button[] {fMarkInternal, fModifyDependencies}, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setEnabledStates();
				doProcessorSetting(e.getSource());
			}
		});
		hookSelectionListener(fTopLevelButtons, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete();
				doProcessorSetting(e.getSource());
			}
		});
		hookSelectionListener(new Button[] {fRemoveImport, fOptionalImport}, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doProcessorSetting(e.getSource());
			}
		});
		hookTextListener(new Text[] {fPackageFilter}, new ModifyListener() {
			public void modifyText(ModifyEvent e) {
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

	private void hookSelectionListener(Button[] buttons, SelectionAdapter adapter) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].addSelectionListener(adapter);
		}
	}

	private void hookTextListener(Text[] texts, ModifyListener listener) {
		for (int i = 0; i < texts.length; i++) {
			texts[i].addModifyListener(listener);
		}
	}

}
