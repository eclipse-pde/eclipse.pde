/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class PluginContentPage extends ContentPage {
    private Label fLabel;
    private Button fYesButton;
    private Button fNoButton;
    
	private ModifyListener classListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (fInitialized)
				fChangedGroups |= P_CLASS_GROUP;
			validatePage();
		}
	};
	private Label fAppIdLabel;
	private Text fAppIdText;
	private Label fAppClassLabel;
	private Text fAppClassText;
	private Group fRCPGroup;

	public PluginContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page,AbstractFieldData data) {
		super(pageName, provider, page, data);
		setTitle(PDEPlugin.getResourceString("ContentPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("ContentPage.desc")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		createPluginPropertiesGroup(container);
		createPluginClassGroup(container);
		createRCPGroup(container);
		
		setControl(container);	
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.NEW_PROJECT_REQUIRED_DATA);
	}

	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(2, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEPlugin.getResourceString("ContentPage.pGroup")); //$NON-NLS-1$

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pid")); //$NON-NLS-1$
		fIdText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pversion")); //$NON-NLS-1$
		fVersionText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pname")); //$NON-NLS-1$
		fNameText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ContentPage.pprovider")); //$NON-NLS-1$
		fProviderText = createText(propertiesGroup, propertiesListener);

		fLibraryLabel = new Label(propertiesGroup, SWT.NONE);
		fLibraryLabel.setText(PDEPlugin.getResourceString("ProjectStructurePage.library")); //$NON-NLS-1$
		fLibraryText = createText(propertiesGroup, propertiesListener);
	}

	private void createPluginClassGroup(Composite container) {
		Group classGroup = new Group(container, SWT.NONE);
		classGroup.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		classGroup.setLayoutData(gd);
		classGroup.setText(PDEPlugin.getResourceString("ContentPage.pClassGroup")); //$NON-NLS-1$

		fGenerateClass = new Button(classGroup, SWT.CHECK);
		fGenerateClass.setText(PDEPlugin.getResourceString("ContentPage.generate")); //$NON-NLS-1$
		fGenerateClass.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fGenerateClass.setLayoutData(gd);
		fGenerateClass.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fClassLabel.setEnabled(fGenerateClass.getSelection());
				fClassText.setEnabled(fGenerateClass.getSelection());
				fUIPlugin.setEnabled(fGenerateClass.getSelection());
				fRCPGroup.setVisible(!fGenerateClass.getSelection() || fUIPlugin.getSelection());
				validatePage();
			}
		});

		fClassLabel = new Label(classGroup, SWT.NONE);
		fClassLabel.setText(PDEPlugin.getResourceString("ContentPage.classname")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fClassLabel.setLayoutData(gd);
		fClassText = createText(classGroup, classListener);

		fUIPlugin = new Button(classGroup, SWT.CHECK);
		fUIPlugin.setText(PDEPlugin.getResourceString("ContentPage.uicontribution")); //$NON-NLS-1$
		fUIPlugin.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 20;
		gd.horizontalSpan = 2;
		fUIPlugin.setLayoutData(gd);
		fUIPlugin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRCPGroup.setVisible(fUIPlugin.getSelection());
				validatePage();
			}
		});
	}

	public void updateData() {
		super.updateData();
		PluginFieldData data = (PluginFieldData)fData;
		data.setClassname(fClassText.getText().trim());
		data.setUIPlugin(fUIPlugin.getSelection());
		data.setDoGenerateClass(fGenerateClass.isEnabled() && fGenerateClass.getSelection());
		data.setRCPApplicationPlugin(!fData.isSimple()
						&& !fData.isLegacy()
						&& fYesButton.getSelection()
						&& (fUIPlugin.getSelection() || !fGenerateClass.getSelection()));
		data.setApplicationID(fAppIdText.getText().trim());
		data.setApplicationClassname(fAppClassText.getText().trim());
	}
	
	private void createRCPGroup(Composite container){
	    fRCPGroup = new Group(container, SWT.NONE);
	    fRCPGroup.setLayout(new GridLayout(2, false));
	    fRCPGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fRCPGroup.setText(PDEPlugin.getResourceString("PluginContentPage.rcpGroup")); //$NON-NLS-1$
	    
	    createRCPQuestion(fRCPGroup, 2);
	    
	    fAppIdLabel = new Label(fRCPGroup, SWT.NONE);
	    fAppIdLabel.setText(PDEPlugin.getResourceString("PluginContentPage.appID")); //$NON-NLS-1$
	    GridData gd = new GridData();
	    gd.horizontalIndent = 20;
	    fAppIdLabel.setLayoutData(gd);
	    fAppIdLabel.setEnabled(false);
	    
	    fAppIdText = createText(fRCPGroup, propertiesListener);
	    fAppIdText.setText("application"); //$NON-NLS-1$
	    fAppIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fAppIdText.setEnabled(false);
	    
	    fAppClassLabel = new Label(fRCPGroup, SWT.NONE);
	    fAppClassLabel.setText(PDEPlugin.getResourceString("PluginContentPage.appClass")); //$NON-NLS-1$
	    gd = new GridData();
	    gd.horizontalIndent = 20;
	    fAppClassLabel.setLayoutData(gd);
	    fAppClassLabel.setEnabled(false);
	    
	    fAppClassText = createText(fRCPGroup, classListener);
	    fAppClassText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fAppClassText.setEnabled(false);
	}
	
	private void createRCPQuestion(Composite parent, int horizontalSpan) {
	    Composite comp = new Composite(parent, SWT.NONE);
	    GridLayout layout = new GridLayout(3, false);
	    layout.marginHeight = layout.marginWidth = 0;
	    comp.setLayout(layout);
	    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	    gd.horizontalSpan = horizontalSpan;
	    comp.setLayoutData(gd);
	    
	    fLabel = new Label(comp, SWT.NONE);
	    fLabel.setText(PDEPlugin.getResourceString("PluginContentPage.appQuestion")); //$NON-NLS-1$
	    fLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    
	    fYesButton = new Button(comp, SWT.RADIO);
	    fYesButton.setText(PDEPlugin.getResourceString("PluginContentPage.yes")); //$NON-NLS-1$
	    fYesButton.setSelection(false);
	    gd = new GridData();
	    gd.widthHint = 50;
	    fYesButton.setLayoutData(gd);
	    fYesButton.addSelectionListener(new SelectionAdapter(){
	        public void widgetSelected(SelectionEvent e) {
	        	boolean enable = fYesButton.getSelection();
	        	fAppIdLabel.setEnabled(enable);
	        	fAppIdText.setEnabled(enable);
	        	fAppClassLabel.setEnabled(enable);
	        	fAppClassText.setEnabled(enable);
	        	validatePage();
	        }
	    });
	    
	    fNoButton = new Button(comp, SWT.RADIO);
	    fNoButton.setText(PDEPlugin.getResourceString("PluginContentPage.no")); //$NON-NLS-1$
	    fNoButton.setSelection(true);
	    gd = new GridData();
	    gd.widthHint = 50;
	    fNoButton.setLayoutData(gd);		
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
    	if (visible) {
    		fMainPage.updateData();
			fGenerateClass.setEnabled(!fData.isSimple());
			fClassLabel.setEnabled(!fData.isSimple() && fGenerateClass.getSelection());
			fClassText.setEnabled(!fData.isSimple() && fGenerateClass.getSelection());
			fUIPlugin.setEnabled(!fData.isSimple() && fGenerateClass.getSelection());

			// plugin class group
			if (((fChangedGroups & P_CLASS_GROUP) == 0)){
				int oldfChanged = fChangedGroups;
				presetClassField(fClassText, computeId(), "Plugin"); //$NON-NLS-1$
				presetClassField(fAppClassText, computeId(), "Application"); //$NON-NLS-1$
				fChangedGroups = oldfChanged;
			}		
			fRCPGroup.setVisible(!fData.isLegacy() && !fData.isSimple()
						&& (fUIPlugin.getSelection() || !fGenerateClass.getSelection()));
    	}
        super.setVisible(visible);
    }
    
	private void presetClassField(Text text, String id, String suffix) {
		StringBuffer buffer = new StringBuffer();
        IStatus status;
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch))
                    buffer.append(ch);
                else if (ch == '.'){
                    status = JavaConventions.validatePackageName(buffer.toString());
                    if (status.getSeverity() == IStatus.ERROR)
                        buffer.append(suffix.toLowerCase());
					buffer.append(ch);
                }
			}
		}
		StringTokenizer tok = new StringTokenizer(buffer.toString(), "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()){
                status = JavaConventions.validatePackageName(buffer.toString());
                if (status.getSeverity() == IStatus.ERROR)
                    buffer.append(suffix.toLowerCase());
				buffer.append("." + Character.toUpperCase(token.charAt(0)) + token.substring(1) + suffix); //$NON-NLS-1$ //$NON-NLS-2$
            }
		}
		text.setText(buffer.toString());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#validatePage()
	 */
	protected void validatePage() {
		setMessage(null);
		String errorMessage = validateProperties();
		if (errorMessage == null && fGenerateClass.isEnabled() && fGenerateClass.getSelection()) {
			IStatus status = JavaConventions.validateJavaTypeName(fClassText.getText().trim());
			if (status.getSeverity() == IStatus.ERROR) {
				errorMessage = status.getMessage();
			} else if (status.getSeverity() == IStatus.WARNING) {
				setMessage(status.getMessage(), DialogPage.WARNING);
			}
		}
		if (errorMessage == null 
				&& !fData.isSimple() && !fData.isLegacy() && fYesButton.getSelection()
				&& (fUIPlugin.getSelection() || !fGenerateClass.getSelection())) {
			IStatus status = JavaConventions.validateJavaTypeName(fAppClassText.getText().trim());
			if (status.getSeverity() == IStatus.ERROR) {
				errorMessage = status.getMessage();
			} else if (status.getSeverity() == IStatus.WARNING) {
				setMessage(status.getMessage(), DialogPage.WARNING);
			}
			if (errorMessage == null)
				errorMessage = validateApplicationID();
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	private String validateApplicationID() {
		String id = fAppIdText.getText().trim();
		if (id.length() == 0)
			return PDEPlugin.getResourceString("PluginContentPage.noApp"); //$NON-NLS-1$

		for (int i = 0; i<id.length(); i++){
			if (!id.substring(i,i+1).matches("[a-zA-Z0-9_]")) //$NON-NLS-1$
				return PDEPlugin.getResourceString("PluginContentPage.invalidAppID"); //$NON-NLS-1$
		}
		return null;
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#getNameFieldQualifier()
	 */
	protected String getNameFieldQualifier() {
		return PDEPlugin.getResourceString("ContentPage.plugin"); //$NON-NLS-1$
	}
}
