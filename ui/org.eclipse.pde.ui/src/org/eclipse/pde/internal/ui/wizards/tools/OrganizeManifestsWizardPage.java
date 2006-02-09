package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class OrganizeManifestsWizardPage extends WizardPage implements IPreferenceConstants, IOrganizeManifestsSettings {
	
	private Button fRemoveUnresolved;
	private Button fAddMissing;
	private Button fMarkInternal;
	private Text fPackageFilter;
	private Label fPackageFilterLabel;
	private Button fRemoveImport;
	private Button fOptionalImport;
	private Button fModifyDependencies;
	private Button fUnusedDependencies;
	private Button fFixIconNLSPaths;
	private Button fRemovedUnusedKeys;
	private Button fRemoveLazy;

	private boolean fWorkToBeDone;
	
	private Button[] fTopLevelButtons; // used for setting page complete state
	private Button[] fParentButtons; // parents with children that need to be dis/enabled
	
	
	private static String title = PDEUIMessages.OrganizeManifestsWizardPage_title;
	protected OrganizeManifestsWizardPage(boolean workToBeDone) {
		super(title);
		setTitle(title);
		setDescription(PDEUIMessages.OrganizeManifestsWizardPage_description);
		fWorkToBeDone = workToBeDone;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		if (fWorkToBeDone) {
			createExportedPackagesGroup(container);
			createRequireImportGroup(container);
			createGeneralGroup(container);
			createNLSGroup(container);
			
			// init
			setButtonArrays();
			presetOptions();
			hookListeners();
		} else {
			Label label = new Label(container, SWT.NONE);
			label.setText(PDEUIMessages.OrganizeManifestsWizardPage_errorMsg);
			setPageComplete(false);
		}
		
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
	}
	
	private void createGeneralGroup(Composite container) {
		Group group = createGroup(container, PDEUIMessages.OrganizeManifestsWizardPage_generalGroup, 1, true);
		
		fRemoveLazy = new Button(group, SWT.CHECK);
		fRemoveLazy.setText(PDEUIMessages.OrganizeManifestsWizardPage_lazyStart);
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
		
		fAddMissing.setSelection(!settings.getBoolean(PROP_ADD_MISSING));
		fMarkInternal.setSelection(!settings.getBoolean(PROP_MARK_INTERNAL));
		String filter = settings.get(PROP_INTERAL_PACKAGE_FILTER);
		fPackageFilter.setText(filter != null ? filter : VALUE_DEFAULT_FILTER);
		fRemoveUnresolved.setSelection(!settings.getBoolean(PROP_REMOVE_UNRESOLVED_EX));
		
		fModifyDependencies.setSelection(!settings.getBoolean(PROP_MODIFY_DEP));
		String resolve = settings.get(PROP_RESOLVE_IMPORTS);
		if (VALUE_IMPORT_OPTIONAL.equals(resolve)) {
			fRemoveImport.setSelection(false);
			fOptionalImport.setSelection(true);
		} else {
			fRemoveImport.setSelection(true);
			fOptionalImport.setSelection(false);
		}
		fUnusedDependencies.setSelection(settings.getBoolean(PROP_UNUSED_DEPENDENCIES));
		
		fRemoveLazy.setSelection(!settings.getBoolean(PROP_REMOVE_LAZY));
		
		fFixIconNLSPaths.setSelection(settings.getBoolean(PROP_NLS_PATH));
		fRemovedUnusedKeys.setSelection(settings.getBoolean(PROP_UNUSED_KEYS));

		setEnabledStates();
		setPageComplete();
	}

	protected void preformOk() {
		if (!fWorkToBeDone)
			return;
		IDialogSettings settings = getDialogSettings();

		settings.put(PROP_ADD_MISSING, !fAddMissing.getSelection());
		settings.put(PROP_MARK_INTERNAL, !fMarkInternal.getSelection());
		settings.put(PROP_INTERAL_PACKAGE_FILTER, fPackageFilter.getText());
		settings.put(PROP_REMOVE_UNRESOLVED_EX, !fRemoveUnresolved.getSelection());
		

		settings.put(PROP_MODIFY_DEP, !fModifyDependencies.getSelection());
		settings.put(PROP_RESOLVE_IMPORTS, fRemoveImport.getSelection() ?
				VALUE_REMOVE_IMPORT : VALUE_IMPORT_OPTIONAL);
		settings.put(PROP_UNUSED_DEPENDENCIES, fUnusedDependencies.getSelection());
		
		settings.put(PROP_REMOVE_LAZY, !fRemoveLazy.getSelection());
		
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
		fTopLevelButtons = new Button[] {
			fRemoveUnresolved, fAddMissing,	fModifyDependencies, fMarkInternal,
			fUnusedDependencies, fFixIconNLSPaths, fRemovedUnusedKeys, fRemoveLazy	
		};
		fParentButtons = new Button[] {
			fMarkInternal, fModifyDependencies
		};
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
		hookListener(fParentButtons, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setEnabledStates();
			}
		});
		hookListener(fTopLevelButtons, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete();
			}
		});
	}
	
	private void hookListener(Button[] buttons, SelectionAdapter adapter) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].addSelectionListener(adapter);
		}
	}
	
	protected IDialogSettings getSettings() {
		return getDialogSettings();
	}
}
