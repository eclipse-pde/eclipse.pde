/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513, 232706
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
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
	private Button fComputeImportPackages;
	private Button fFixIconNLSPaths;
	private Button fRemovedUnusedKeys;
	private Button fRemoveLazy;
	private Button fRemoveUselessFiles;
	private Button fUpdateBree;

	/**
	 * I will need to review Jason's work in detail, as he has expressed
	 * uncertainty about the commits he made. It’s important to ensure nothing
	 * breaks before proceeding further.
	 *
	 * – Comment added by Jose Rodriguez
	 */

	// belongs to general section

	private Button[] fTopLevelButtons; // used for setting page complete state

	private OrganizeManifestsProcessor fProcessor;
	private final Set<IProject> fCustomProjects;

	private static String title = PDEUIMessages.OrganizeManifestsWizardPage_title;

	protected OrganizeManifestsWizardPage(java.util.Set<IProject> customProjects) {
		super(title);
		setTitle(title);
		setDescription(PDEUIMessages.OrganizeManifestsWizardPage_description);
		fCustomProjects = customProjects;
	}

	@Override
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

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

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
			message = NLS.bind(PDEUIMessages.OrganizeManifestsWizardPage_ProjectsUsingCustomBuildWarning, fCustomProjects.iterator().next().getName());
		} else {
			StringBuilder buf = new StringBuilder();
			for (Iterator<IProject> iterator = fCustomProjects.iterator(); iterator.hasNext();) {
				IProject project = iterator.next();
				buf.append(project.getName());
				if (iterator.hasNext()) {
					buf.append(',').append(' ');
				}
			}
			message = NLS.bind(PDEUIMessages.OrganizeManifestsWizardPage_ProjectsUsingCustomBuildWarningPlural, buf.toString());
		}

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

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

		fComputeImportPackages = new Button(group, SWT.CHECK);
		fComputeImportPackages.setText(PDEUIMessages.OrganizeManifestsWizardPage_computeImports);
	}

	private void createGeneralGroup(Composite container) {
		Group group = SWTFactory.createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_generalGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fRemoveLazy = new Button(group, SWT.CHECK);
		fRemoveLazy.setText(PDEUIMessages.OrganizeManifestsWizardPage_lazyStart);

		fRemoveUselessFiles = new Button(group, SWT.CHECK);
		fRemoveUselessFiles.setText(PDEUIMessages.OrganizeManifestsWizardPage_uselessPluginFile);

		fUpdateBree = new Button(group, SWT.CHECK);
		fUpdateBree.setText(PDEUIMessages.OrganizeManifestsWizardPage_updateBREE);

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

		// PLACEHOLDER
																							// BY
																							// JASON.
		// AGAIN PLACEHOLDER ABOVE PDEUIMESSAGES. LOOK INTO CREATING SOMETHING
		// LIKE 'lazyStart' OR 'uselessPluginFile'
		// WITHIN THE CLASS 'PDEUIMessages'.

		// Update, I believe I have fixed the placeholder, will keep notes here
		// just in case.
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
		if (filter == null) {
			filter = VALUE_DEFAULT_FILTER;
		}
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

		selection = settings.getBoolean(PROP_COMPUTE_IMPORTS);
		fComputeImportPackages.setSelection(selection);
		fProcessor.setComputeImports(selection);

		selection = !settings.getBoolean(PROP_REMOVE_LAZY);
		fRemoveLazy.setSelection(selection);
		fProcessor.setRemoveLazy(selection);

		selection = !settings.getBoolean(PROP_REMOVE_USELESSFILES);
		fRemoveUselessFiles.setSelection(selection);
		fProcessor.setRemoveUselessFiles(selection);

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

		// remove bree feature, not sure if it is supposed to include a '!'
		// operator,
		// so I am just following the general group's syntax

		selection = !settings.getBoolean(PROP_UPDATE_BREE);
		fUpdateBree.setSelection(selection);
		fProcessor.setRemoveUselessFiles(selection);

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

		// ANOTHER PLACEHOLDER METHOD. Look into
		// OrganizeManifestsProcessor.java
		// and create a method similar to '.setRemoveUselessFiles' &
		// 'setRemoveLazy'.

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
		settings.put(PROP_COMPUTE_IMPORTS, fComputeImportPackages.getSelection());

		settings.put(PROP_REMOVE_LAZY, !fRemoveLazy.getSelection());
		settings.put(PROP_REMOVE_USELESSFILES, !fRemoveUselessFiles.getSelection());
		settings.put(PROP_UPDATE_BREE, !fUpdateBree.getSelection());

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

		// Not
																		// entirely
																		// sure,
																		// see
																		// below.
		// I followed the structure of this method, and crossed reference w/
		// presetOptions() to see if button's that use a '!' there are also used
		// here in performOk(). I evaluated this as true. Though, since my
		// button fUpdateBree
		// didn't exist in the first place, I am not sure if I should also use a
		// '!' on it.

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
		fTopLevelButtons = new Button[] { fRemoveUnresolved, fAddMissing, fModifyDependencies, fMarkInternal,
				fUnusedDependencies, fAdditonalDependencies, fComputeImportPackages, fFixIconNLSPaths,
				fRemovedUnusedKeys, fRemoveLazy, fRemoveUselessFiles, fUpdateBree, fCalculateUses };

		/**
		 * I will need to review Jason's work in detail, as he has expressed
		 * uncertainty about the commits he made. It’s important to ensure
		 * nothing breaks before proceeding further.
		 *
		 * – Comment added by Jose Rodriguez
		 */

		// added 'fUpdateBree' right after our existing general group
	}

	private void setPageComplete() {
		boolean pageComplete = false;
		for (Button button : fTopLevelButtons) {
			if (button.getSelection()) {
				pageComplete = true;
				break;
			}
		}
		setPageComplete(pageComplete);
	}

	private void hookListeners() {
		hookSelectionListener(new Button[] { fMarkInternal, fModifyDependencies }, widgetSelectedAdapter(e -> {
			setEnabledStates();
			doProcessorSetting(e.getSource());
		}));
		hookSelectionListener(fTopLevelButtons, widgetSelectedAdapter(e -> {
			setPageComplete();
			doProcessorSetting(e.getSource());
		}));
		hookSelectionListener(new Button[] { fRemoveImport, fOptionalImport },
				widgetSelectedAdapter(e -> doProcessorSetting(e.getSource())));
		hookTextListener(new Text[] {fPackageFilter}, e -> doProcessorSetting(e.getSource()));
	}

	/**
	 * According to GROK4, we are making progress, but there is still
	 * information that needs to be traced and reviewed. For now, I will not
	 * delete anything to avoid disrupting work for other teammates. It’s more
	 * professional to comment out changes when unsure, in case any mistakes are
	 * made during the process. I will highlight areas with comments instead.
	 *
	 * – Comment added by Jose Rodriguez
	 */

	private void doProcessorSetting(Object source) {
		if (fProcessor == null) {
			return;
		}
		if (fAddMissing.equals(source)) {
			fProcessor.setAddMissing(fAddMissing.getSelection());
		} else if (fMarkInternal.equals(source)) {
			fProcessor.setMarkInternal(fMarkInternal.getSelection());
		} else if (fPackageFilter.equals(source)) {
			fProcessor.setPackageFilter(fPackageFilter.getText());
		} else if (fRemoveUnresolved.equals(source)) {
			fProcessor.setRemoveUnresolved(fRemoveUnresolved.getSelection());
		} else if (fCalculateUses.equals(source)) {
			fProcessor.setCalculateUses(fCalculateUses.getSelection());
		} else if (fModifyDependencies.equals(source)) {
			fProcessor.setModifyDep(fModifyDependencies.getSelection());
		} else if (fOptionalImport.equals(source)) {
			fProcessor.setRemoveDependencies(!fOptionalImport.getSelection());
		} else if (fRemoveImport.equals(source)) {
			fProcessor.setRemoveDependencies(fRemoveImport.getSelection());
		} else if (fUnusedDependencies.equals(source)) {
			fProcessor.setUnusedDependencies(fUnusedDependencies.getSelection());
		} else if (fAdditonalDependencies.equals(source)) {
			fProcessor.setAddDependencies(fAdditonalDependencies.getSelection());
		} else if (fComputeImportPackages.equals(source)) {
			fProcessor.setAddDependencies(fComputeImportPackages.getSelection());
		} else if (fRemoveLazy.equals(source)) {
			fProcessor.setRemoveLazy(fRemoveLazy.getSelection());
		} else if (fRemoveUselessFiles.equals(source)) {
			fProcessor.setRemoveUselessFiles(fRemoveUselessFiles.getSelection());
		} else if (fUpdateBree.equals(source)) {
			fProcessor.setRemoveUselessFiles(fUpdateBree.getSelection());

			/**
			 * I will need to review Jason's work in detail, as he has expressed
			 * uncertainty about the commits he made. It’s important to ensure
			 * nothing breaks before proceeding further.
			 *
			 * – Comment added by Jose Rodriguez
			 */

			// ABOVE IS A PLACEHOLDER, KEEP IN MIND.
			// We will have to look into OrganizeManifestsProcessor.java
			// in order to create our own setter for the update BREE feature.

			/**
			 * I will need to review Jason's work in detail, as he has expressed
			 * uncertainty about the commits he made. It’s important to ensure
			 * nothing breaks before proceeding further.
			 *
			 * – Comment added by Jose Rodriguez
			 */

		} else if (fFixIconNLSPaths.equals(source)) {
			fProcessor.setPrefixIconNL(fFixIconNLSPaths.getSelection());
		} else if (fRemovedUnusedKeys.equals(source)) {
			fProcessor.setUnusedKeys(fRemovedUnusedKeys.getSelection());
		}
	}

	private void hookSelectionListener(Button[] buttons, SelectionListener adapter) {
		for (Button button : buttons) {
			button.addSelectionListener(adapter);
		}
	}

	private void hookTextListener(Text[] texts, ModifyListener listener) {
		for (Text text : texts) {
			text.addModifyListener(listener);
		}
	}

}
