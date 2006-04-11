/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;


public class NewProjectCreationPage extends WizardNewProjectCreationPage {
	protected Button fJavaButton;
	protected boolean fFragment;
	private Label fSourceLabel;
	private Text fSourceText;
	private Label fOutputlabel;
	private Text fOutputText;
	private AbstractFieldData fData;
	private Button fEclipseButton;
	private Combo fTargetCombo;
	private Combo fOSGiCombo;
	private Button fOSGIButton;
	
	public NewProjectCreationPage(String pageName, AbstractFieldData data, boolean fragment){
		super(pageName);
		fFragment = fragment;
		fData = data;
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite)getControl();
		GridLayout layout = new GridLayout();
		control.setLayout(layout);

		createProjectTypeGroup(control);
		createFormatGroup(control);
		
		updateRuntimeDependency();

		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control,
				fFragment ? IHelpContextIds.NEW_FRAGMENT_STRUCTURE_PAGE
							: IHelpContextIds.NEW_PROJECT_STRUCTURE_PAGE);
		setControl(control);
	}
	
	private void createProjectTypeGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.ProjectStructurePage_settings); 
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	
		fJavaButton = createButton(group, SWT.CHECK, 2, 0);
		fJavaButton.setText(PDEUIMessages.ProjectStructurePage_java); 
		fJavaButton.setSelection(true);
		fJavaButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fJavaButton.getSelection();
				fSourceLabel.setEnabled(enabled);
				fSourceText.setEnabled(enabled);
				fOutputlabel.setEnabled(enabled);
				fOutputText.setEnabled(enabled);
				validatePage();
			}
		});
		
		fSourceLabel = createLabel(group, PDEUIMessages.ProjectStructurePage_source); 
		fSourceText = createText(group);
		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		fSourceText.setText(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
		
		fOutputlabel = createLabel(group, PDEUIMessages.ProjectStructurePage_output); 
		fOutputText = createText(group);		
		fOutputText.setText(store.getString(PreferenceConstants.SRCBIN_BINNAME));
	}
	
	private void createFormatGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.NewProjectCreationPage_target);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(group, SWT.NONE);
		if (fFragment)
			label.setText(PDEUIMessages.NewProjectCreationPage_ftarget);
		else
			label.setText(PDEUIMessages.NewProjectCreationPage_ptarget);			
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		    
		fEclipseButton = createButton(group, SWT.RADIO, 1, 30);
    	fEclipseButton.setText(PDEUIMessages.NewProjectCreationPage_pDependsOnRuntime);	    
	    fEclipseButton.setSelection(fData.getOSGiFramework() == null);
	    fEclipseButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateRuntimeDependency();
			}
		});
		
		fTargetCombo = new Combo(group, SWT.READ_ONLY|SWT.SINGLE);
		fTargetCombo.setItems(new String[] {ICoreConstants.TARGET32, ICoreConstants.TARGET31, ICoreConstants.TARGET30});
		fTargetCombo.setText(TargetPlatform.getTargetVersionString());
		
	    fOSGIButton = createButton(group, SWT.RADIO, 1, 30);
    	fOSGIButton.setText(PDEUIMessages.NewProjectCreationPage_pPureOSGi); 	   
	    fOSGIButton.setSelection(fData.getOSGiFramework() != null);
	    
		fOSGiCombo = new Combo(group, SWT.READ_ONLY|SWT.SINGLE);
		fOSGiCombo.setItems(new String[] {ICoreConstants.EQUINOX, PDEUIMessages.NewProjectCreationPage_standard}); 
		fOSGiCombo.setText(ICoreConstants.EQUINOX);	
		
	}
	
	private void updateRuntimeDependency() {
		boolean depends = fEclipseButton.getSelection();
		fTargetCombo.setEnabled(depends);
		fOSGiCombo.setEnabled(!depends);
	}
	
	private Button createButton(Composite container, int style, int span, int indent) {
		Button button = new Button(container, style);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		button.setLayoutData(gd);
		return button;		
	}
	
	private Label createLabel(Composite container, String text) {
		Label label = new Label(container, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 30;
		label.setLayoutData(gd);
		return label;
	}
	
	private Text createText(Composite container) {
		Text text = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(validatePage());
			}
		});
		return text;
	}
	
	public void updateData() {
		fData.setSimple(!fJavaButton.getSelection());
		fData.setSourceFolderName(fSourceText.getText().trim());
		fData.setOutputFolderName(fOutputText.getText().trim());
		fData.setLegacy(false); //$NON-NLS-1$
		fData.setTargetVersion(fTargetCombo.getText());
		fData.setHasBundleStructure(fOSGIButton.getSelection() || Double.parseDouble(fTargetCombo.getText()) >= 3.1);	
		fData.setOSGiFramework(fOSGIButton.getSelection() ? fOSGiCombo.getText() : null);
	}
	
    protected boolean validatePage() {
    	if (!super.validatePage())
    		return false;
    	
    	String name = getProjectName();
    	if (name.indexOf('%') > 0) {
    		setErrorMessage(PDEUIMessages.NewProjectCreationPage_invalidProjectName);
    		return false;
    	}
    	
    	String location = getLocationPath().toString();
    	if (location.indexOf('%') > 0) {
    		setErrorMessage(PDEUIMessages.NewProjectCreationPage_invalidLocationPath);
    		return false;
    	}
    	
    	IWorkspace workspace = ResourcesPlugin.getWorkspace();
    	IProject dmy = workspace.getRoot().getProject("project"); //$NON-NLS-1$
    	IStatus status;
    	if(fSourceText != null && fSourceText.getText().length() != 0) {
    		status = workspace.validatePath(dmy.getFullPath().append(fSourceText.getText()).toString(), IResource.FOLDER);
    		if (!status.isOK()) {
				setErrorMessage(status.getMessage());
				return false;
    		}
    	}
    	if(fOutputText != null && fOutputText.getText().length() != 0) {
    		status = workspace.validatePath(dmy.getFullPath().append(fOutputText.getText()).toString(), IResource.FOLDER);
    		if (!status.isOK()) {
				setErrorMessage(status.getMessage());
				return false;
    		}
    	}
    	setErrorMessage(null);
    	setMessage(null);
    	return true;
    }
	
}
