/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
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
	protected boolean fIsFragment;
	private Label fSourceLabel;
	private Text fSourceText;
	private Label fOutputlabel;
	private Text fOutputText;
	private AbstractFieldData fData;
	private Combo fTargetCombo;
	private Button fRuntimeDepButton;
	private Label fTargetLabel;
	private Label fOSGiLabel;
	private Combo fOSGiCombo;
	private Button fOSGIButton;
	
	public NewProjectCreationPage(String pageName, AbstractFieldData data, boolean isFragment){
		super(pageName);
		fIsFragment = isFragment;
		fData = data;
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite)getControl();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		control.setLayout(layout);

		createProjectTypeGroup(control);
		createFormatGroup(control);
		
		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control,
				fIsFragment ? IHelpContextIds.NEW_FRAGMENT_STRUCTURE_PAGE
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
	
		fJavaButton = createButton(group, SWT.CHECK);
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
		if (fIsFragment)
			group.setText(PDEUIMessages.ProjectStructurePage_fformat); 
		else
			group.setText(PDEUIMessages.ProjectStructurePage_pformat); 			
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    
		fRuntimeDepButton = createButton(group, SWT.RADIO);
	    if (fIsFragment)
	    	fRuntimeDepButton.setText(PDEUIMessages.NewProjectCreationPage_fDependsOnRuntime);
	    else 
	    	fRuntimeDepButton.setText(PDEUIMessages.NewProjectCreationPage_pDependsOnRuntime);	    
	    fRuntimeDepButton.setSelection(fData.getOSGiFramework() == null);
	    fRuntimeDepButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateRuntimeDependency();
			}
		});
		
		fTargetLabel = new Label(group, SWT.NONE);
		if (fIsFragment)
			fTargetLabel.setText(PDEUIMessages.ProjectStructurePage_fTarget);
		else
			fTargetLabel.setText(PDEUIMessages.ProjectStructurePage_pTarget);
		GridData gd = new GridData();
		gd.horizontalIndent = 30;
		fTargetLabel.setLayoutData(gd);
		
		fTargetCombo = new Combo(group, SWT.READ_ONLY|SWT.SINGLE);
		fTargetCombo.setItems(new String[] {ICoreConstants.TARGET32,
											ICoreConstants.TARGET31, 
											ICoreConstants.TARGET30, 
											ICoreConstants.TARGET21});
		fTargetCombo.setText(PDECore.getDefault().getTargetVersion());
		
	    fOSGIButton = createButton(group, SWT.RADIO);
	    if (fIsFragment)
	    	fOSGIButton.setText(PDEUIMessages.NewProjectCreationPage_fPureOSGi); 
	    else
	    	fOSGIButton.setText(PDEUIMessages.NewProjectCreationPage_pPureOSGi); 	
	   
	    fOSGIButton.setSelection(fData.getOSGiFramework() != null);
	    
		fOSGiLabel = new Label(group, SWT.NONE);
		if (fIsFragment)
			fOSGiLabel.setText(PDEUIMessages.NewProjectCreationPage_fFrameworkTarget);
		else
			fOSGiLabel.setText(PDEUIMessages.NewProjectCreationPage_pFrameworkTarget);
		gd = new GridData();
		gd.horizontalIndent = 30;
		fOSGiLabel.setLayoutData(gd);
		
		fOSGiCombo = new Combo(group, SWT.READ_ONLY|SWT.SINGLE);
		fOSGiCombo.setItems(new String[] {ICoreConstants.EQUINOX, PDEUIMessages.NewProjectCreationPage_standard}); 
		fOSGiCombo.setText(ICoreConstants.EQUINOX);	
		
	    updateRuntimeDependency();
	}
	
	private void updateRuntimeDependency() {
		boolean depends = fRuntimeDepButton.getSelection();
		fTargetLabel.setEnabled(depends);
		fTargetCombo.setEnabled(depends);
		fOSGiLabel.setEnabled(!depends);
		fOSGiCombo.setEnabled(!depends);
	}
	
	private Button createButton(Composite container, int style) {
		Button button = new Button(container, style);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
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
				validatePage();
			}
		});
		return text;
	}
	
	public void updateData() {
		fData.setSimple(!fJavaButton.getSelection());
		fData.setSourceFolderName(fSourceText.getText().trim());
		fData.setOutputFolderName(fOutputText.getText().trim());
		fData.setLegacy(fTargetCombo.getText().equals("2.1")); //$NON-NLS-1$
		fData.setTargetVersion(fTargetCombo.getText());
		fData.setHasBundleStructure(hasBundleStructure());	
		fData.setOSGiFramework(fOSGIButton.getSelection() ? fOSGiCombo.getText() : null);
	}
	
	private boolean hasBundleStructure() {
		if (fOSGIButton.getSelection())
			return true;
		String version = fTargetCombo.getText();
		return !version.equals("2.1") && !version.equals("3.0"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
