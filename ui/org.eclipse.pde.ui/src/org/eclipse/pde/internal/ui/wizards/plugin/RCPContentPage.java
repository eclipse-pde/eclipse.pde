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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class RCPContentPage extends WizardPage {
	
    private Text fAppText;
    private Text fClassText;
    
    private Text fProductText;
    protected Button fUseDefaultButton;
    protected IProjectProvider fProvider;
    protected RCPData fData;
    private boolean fProductChanged = false;
    protected boolean fInitialized = false;
    
	private Button fYesbutton;
	private Button fNoButton;
	private Label fProductLabel;
	private Button fTemplateFilesButton;
	private boolean fClassChanged;

    public RCPContentPage(String pageName, IProjectProvider provider) {
        super(pageName);
        setTitle(PDEPlugin.getResourceString("RCPContentPage.title")); //$NON-NLS-1$
        setDescription(PDEPlugin.getResourceString("RCPContentPage.desc")); //$NON-NLS-1$
        fData = new RCPData();
        fProvider = provider;
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 15;
        container.setLayout(layout);
        createApplicationGroup(container);
        createProductGroup(container);
        Dialog.applyDialogFont(container);
        setControl(container);
    }

    private void createApplicationGroup(Composite container) {
        Group group = new Group(container, SWT.NONE);
        group.setText(PDEPlugin.getResourceString(PDEPlugin.getResourceString("RCPContentPage.appGroup"))); //$NON-NLS-1$
        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 8;      
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(group, SWT.WRAP);
        label.setText(PDEPlugin.getResourceString("RCPContentPage.appLabel")); //$NON-NLS-1$
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 450;
        label.setLayoutData(gd);
        
        label = new Label(group, SWT.NONE);
        label.setText(PDEPlugin.getResourceString("RCPContentPage.appID")); //$NON-NLS-1$
        
        fAppText = new Text(group, SWT.BORDER|SWT.SINGLE);
        fAppText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fAppText.setText("application"); //$NON-NLS-1$
        fAppText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
        
        label = new Label(group, SWT.NONE);
        label.setText(PDEPlugin.getResourceString("RCPContentPage.appClass")); //$NON-NLS-1$
        
        fClassText = new Text(group, SWT.BORDER|SWT.SINGLE);
        fClassText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fClassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
				fClassChanged = true;
			}
		});
    }

    private void createProductGroup(Composite container) {
        Group group = new Group(container, SWT.NONE);
        group.setText(PDEPlugin.getResourceString("RCPContentPage.productBranding")); //$NON-NLS-1$
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 8;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        createBrandingComposite(group);
        createProductText(group);
        
        fUseDefaultButton = new Button(group, SWT.CHECK);       
        fUseDefaultButton.setText(PDEPlugin.getResourceString("RCPContentPage.useDefaultImages")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.horizontalIndent = 15;
        fUseDefaultButton.setLayoutData(gd);
        fUseDefaultButton.setSelection(true);
        
        fTemplateFilesButton = new Button(group, SWT.CHECK);
        fTemplateFilesButton.setText(PDEPlugin.getResourceString("RCPContentPage.generateFiles")); //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalIndent = 15;
        fTemplateFilesButton.setLayoutData(gd);
        fTemplateFilesButton.setSelection(true);
    }
    
    private void createBrandingComposite(Composite parent) {
    	Composite container = new Composite(parent, SWT.NONE);
    	GridLayout layout = new GridLayout(3, false);
    	layout.marginHeight = layout.marginWidth = 0;
    	container.setLayout(layout);
    	container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	
    	Label label = new Label(container, SWT.NONE);
    	label.setText(PDEPlugin.getResourceString("RCPContentPage.brandingQuestion")); //$NON-NLS-1$
    	label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	
    	fYesbutton = new Button(container, SWT.RADIO);
    	fYesbutton.setText(PDEPlugin.getResourceString("RCPContentPage.yes")); //$NON-NLS-1$
    	GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
    	gd.widthHint = 50;
    	fYesbutton.setLayoutData(gd);
    	fYesbutton.setSelection(true);
    	fYesbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fYesbutton.getSelection();
				fProductLabel.setEnabled(selected);
				fProductText.setEnabled(selected);
				fUseDefaultButton.setEnabled(selected);
				fTemplateFilesButton.setEnabled(selected);
				validatePage();
			}});
     	
    	fNoButton = new Button(container, SWT.RADIO);
    	fNoButton.setText(PDEPlugin.getResourceString("RCPContentPage.no")); //$NON-NLS-1$
    	gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
    	gd.widthHint = 50;
    	fNoButton.setLayoutData(gd);
    }
    
    private void createProductText(Composite parent) {
    	Composite container = new Composite(parent, SWT.NONE);
    	GridLayout layout = new GridLayout(2, false);
    	layout.marginHeight = layout.marginWidth = 0;
    	container.setLayout(layout);
     	container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	
        fProductLabel = new Label(container, SWT.NONE);       
        fProductLabel.setText(PDEPlugin.getResourceString("RCPContentPage.productName")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.horizontalIndent = 15;
        fProductLabel.setLayoutData(gd);
        
        fProductText = new Text(container, SWT.BORDER|SWT.SINGLE);
        fProductText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); 
        fProductText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fProductChanged = true;
				validatePage();
			}
		});
    }

    public void updateData() {
        fData.setApplicationId(fAppText.getText().trim());
        fData.setApplicationClass(fClassText.getText().trim());
        
        fData.setAddBranding(fYesbutton.getSelection());
        fData.setProductName(fProductText.getText().trim());
        fData.setUseDefaultImages(fUseDefaultButton.getSelection());
        fData.setGenerateTemplateFiles(fTemplateFilesButton.getSelection());
    }

    public RCPData getBrandingData() {
        return fData;
    }

    public void validatePage() {
		String errorMessage = null;
		setMessage(null);
		String applicationID = fAppText.getText().trim();
		if (applicationID.length() == 0) {
			errorMessage = PDEPlugin.getResourceString("RCPContentPage.appIDNotSet"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < applicationID.length(); i++) {
				if (applicationID.substring(i,i+1).matches("[^A-Ya-z0-9_]")) { //$NON-NLS-1$
					errorMessage = PDEPlugin.getResourceString("RCPContentPage.invalidID"); //$NON-NLS-1$
					break;
				}
			}
		}
		
		// validate Java class name
		IStatus status = JavaConventions.validateJavaTypeName(fClassText.getText().trim());
		if (status.getSeverity() == IStatus.ERROR) {
			errorMessage = status.getMessage();
		} else if (status.getSeverity() == IStatus.WARNING) {
			setMessage(status.getMessage(), DialogPage.WARNING);
		}
		
		// ensure product name is set if branding option is selected
		if (errorMessage == null) {
			if (fYesbutton.getSelection()) {
				if (fProductText.getText().trim().length() == 0)
					errorMessage = PDEPlugin.getResourceString("RCPContentPage.prodNameNotSet"); //$NON-NLS-1$
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(getErrorMessage() == null);
	}
    
	private void presetClassField(String id) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.')
					buffer.append(ch);
			}
		}
		StringTokenizer tok = new StringTokenizer(buffer.toString(), "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens())
				buffer.append("." + Character.toUpperCase(token.charAt(0)) + token.substring(1) + "Application"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fClassText.setText(buffer.toString());
	}


    /*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible)
            return;
        if (!fInitialized) {
            presetProductNameField(fProvider.getProjectName());
            presetClassField(computeId());
            fInitialized = true;
            fProductChanged = false;
            fClassChanged = false;
        } else if (!fProductChanged) {
            presetProductNameField(fProvider.getProjectName());
            fProductChanged = false;
        }  else if (!fClassChanged) {
        	presetClassField(computeId());
        	fClassChanged = false;
        }
        validatePage();
    }
    
	private String computeId() {
		return fProvider.getProjectName().replaceAll("[^a-zA-Z0-9\\._]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

    private void presetProductNameField(String id) {
        int index = id.lastIndexOf("."); //$NON-NLS-1$
        if (index != -1 && index!= id.length()-1)
            id = id.substring(index+1, id.length());
        String productName=""; //$NON-NLS-1$
        StringTokenizer tok = new StringTokenizer(id, " "); //$NON-NLS-1$
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (productName.length()!= 0)
                productName = productName + " "; //$NON-NLS-1$
            productName = productName + Character.toUpperCase(token.charAt(0)) + ((token.length() > 1) ? token.substring(1) : ""); //$NON-NLS-1$
        }
        productName = productName + ((productName.length()>0) ? " Product" : "Product"); //$NON-NLS-1$ //$NON-NLS-2$
        fProductText.setText(productName);
    }}
