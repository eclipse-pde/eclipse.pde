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

import java.util.StringTokenizer;

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
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

public class NewLibraryPluginCreationPage extends WizardNewProjectCreationPage {

	class PropertiesListener implements ModifyListener {
		private boolean fBlocked = false;

		private boolean fChanged = false;

		public boolean isChanged() {
			return fChanged;
		}

		public void modifyText(ModifyEvent e) {
			if (!fBlocked) {
				fChanged = true;
				validatePage();
			}
		}

		public void setBlocked(boolean blocked) {
			this.fBlocked = blocked;
		}
	}

	private LibraryPluginFieldData fData;
	protected NewLibraryPluginCreationPage fMainPage;
	protected IProjectProvider fProjectProvider;
	protected PropertiesListener fPropertiesListener = new PropertiesListener();
	protected Text fIdText;
	protected Text fNameText;
	protected Text fProviderText;
	protected Text fVersionText;
	private Button fEclipseButton;
	private Button fOSGIButton;
	private Combo fOSGiCombo;
	private Combo fTargetCombo;
	protected Button fJarredCheck;

	public NewLibraryPluginCreationPage(String pageName, LibraryPluginFieldData data) {
		super(pageName);
		fData = data;
		setTitle(PDEUIMessages.NewLibraryPluginCreationPage_title); 
		setDescription(PDEUIMessages.NewLibraryPluginCreationPage_desc); 
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		control.setLayout(layout);

		createPluginPropertiesGroup(control);

		createFormatGroup(control);
		
		updateRuntimeDependency();

		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control,
				IHelpContextIds.NEW_LIBRARY_PROJECT_STRUCTURE_PAGE);
		setControl(control);
	}

	private void createFormatGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.NewProjectCreationPage_target);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(group, SWT.NONE);
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
		
		fJarredCheck = new Button(group, SWT.CHECK);
		fJarredCheck.setText(PDEUIMessages.NewLibraryPluginCreationPage_jarred); 
		gd = new GridData();
		gd.horizontalSpan = 2;
		fJarredCheck.setLayoutData(gd);
		fJarredCheck.setSelection(Double.parseDouble(fTargetCombo.getText()) >= 3.1);
	}
	
	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(2, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.NewLibraryPluginCreationPage_pGroup); 

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pid); 
		fIdText = createText(propertiesGroup, fPropertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pversion); 
		fVersionText = createText(propertiesGroup, fPropertiesListener);
		fPropertiesListener.setBlocked(true);
		fVersionText.setText("1.0.0"); //$NON-NLS-1$
		fPropertiesListener.setBlocked(false);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pname); 
		fNameText = createText(propertiesGroup, fPropertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pprovider); 
		fProviderText = createText(propertiesGroup, fPropertiesListener);

	}

	protected Text createText(Composite parent, ModifyListener listener) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(listener);
		return text;
	}

	protected String getNameFieldQualifier() {
		return PDEUIMessages.NewLibraryPluginCreationPage_plugin; 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		updateData();
		return super.getNextPage();
	}

	protected boolean isVersionValid(String version) {
		try {
			new PluginVersionIdentifier(version);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private void presetNameField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		if (!tok.hasMoreTokens()) {
			fNameText.setText(""); //$NON-NLS-1$
			return;
		}
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()) {
				fNameText.setText(Character.toUpperCase(token.charAt(0))
						+ ((token.length() > 1) ? token.substring(1) : "") //$NON-NLS-1$
						+ " " + getNameFieldQualifier()); //$NON-NLS-1$
			}
		}
	}

	public void updateData() {
		fData.setSimple(false);
		fData.setSourceFolderName(null);
		fData.setOutputFolderName(null);
		fData.setLegacy(false);
		fData.setTargetVersion(fTargetCombo.getText());

		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		fData.setLibraryName(null);
		fData.setHasBundleStructure(fOSGIButton.getSelection() || Double.parseDouble(fTargetCombo.getText()) >= 3.1);	
		fData.setOSGiFramework(fOSGIButton.getSelection() ? fOSGiCombo.getText() : null);
		fData.setUnzipLibraries(fJarredCheck.isEnabled()
				&& fJarredCheck.getSelection());
		
		PluginFieldData data = fData;
		data.setClassname(null);
		data.setUIPlugin(false);
		data.setDoGenerateClass(false);
		data.setRCPApplicationPlugin(false);
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.NewLibraryPluginCreationPage_noid; 

		if (!IdUtil.isValidCompositeID(id)) { 
			return PDEUIMessages.NewLibraryPluginCreationPage_invalidId; 
		}
		return null;
	}

	protected boolean validatePage() {
		String id = IdUtil.getValidId(getProjectName());
		// properties group
		if (!fPropertiesListener.isChanged() && fIdText != null) {
			fPropertiesListener.setBlocked(true);
			fIdText.setText(id);
			presetNameField(id);
			fPropertiesListener.setBlocked(false);
		}

		if (!super.validatePage())
			return false;
		setMessage(null);
		String errorMessage = validateProperties();
		setErrorMessage(errorMessage);
		return errorMessage == null;
	}

	protected String validateProperties() {
		String errorMessage = validateId();
		if (errorMessage != null)
			return errorMessage;

		if (fVersionText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noversion; 
		} else if (!isVersionValid(fVersionText.getText().trim())) {
			errorMessage = PDEUIMessages.ContentPage_badversion; 
		} else if (fNameText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noname; 
		}

		if (errorMessage != null)
			return errorMessage;

		return errorMessage;
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
}
