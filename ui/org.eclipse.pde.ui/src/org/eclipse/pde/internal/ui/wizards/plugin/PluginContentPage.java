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

import java.util.Locale;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.pde.internal.core.ICoreConstants;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class PluginContentPage extends ContentPage {
	protected final static int P_CLASS_GROUP = 2;

	private Text fClassText;
	private Button fGenerateClass;
	private Button fUIPlugin;
	private Label fClassLabel;

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
	
	private Group fRCPGroup;

	public PluginContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page,AbstractFieldData data) {
		super(pageName, provider, page, data);
		setTitle(PDEUIMessages.ContentPage_title); 
		setDescription(PDEUIMessages.ContentPage_desc); 
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
		
		Dialog.applyDialogFont(container);
		setControl(container);	
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_PROJECT_REQUIRED_DATA);
	}

	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(2, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.ContentPage_pGroup); 

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pid); 
		fIdText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pversion); 
		fVersionText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pname); 
		fNameText = createText(propertiesGroup, propertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.ContentPage_pprovider); 
		fProviderText = createText(propertiesGroup, propertiesListener);

		fLibraryLabel = new Label(propertiesGroup, SWT.NONE);
		fLibraryLabel.setText(PDEUIMessages.ProjectStructurePage_library); 
		fLibraryText = createText(propertiesGroup, propertiesListener);
	}

	private void createPluginClassGroup(Composite container) {
		Group classGroup = new Group(container, SWT.NONE);
		classGroup.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		classGroup.setLayoutData(gd);
		classGroup.setText(PDEUIMessages.ContentPage_pClassGroup); 

		fGenerateClass = new Button(classGroup, SWT.CHECK);
		fGenerateClass.setText(PDEUIMessages.ContentPage_generate); 
		fGenerateClass.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fGenerateClass.setLayoutData(gd);
		fGenerateClass.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fClassLabel.setEnabled(fGenerateClass.getSelection());
				fClassText.setEnabled(fGenerateClass.getSelection());
				updateData();
				validatePage();
			}
		});

		fClassLabel = new Label(classGroup, SWT.NONE);
		fClassLabel.setText(PDEUIMessages.ContentPage_classname); 
		gd = new GridData();
		gd.horizontalIndent = 20;
		fClassLabel.setLayoutData(gd);
		fClassText = createText(classGroup, classListener);

		fUIPlugin = new Button(classGroup, SWT.CHECK);
		fUIPlugin.setText(PDEUIMessages.ContentPage_uicontribution); 
		fUIPlugin.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fUIPlugin.setLayoutData(gd);
		fUIPlugin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateData();
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
						&& fYesButton.getSelection());
	}
	
	private void createRCPGroup(Composite container){
	    fRCPGroup = new Group(container, SWT.NONE);
	    fRCPGroup.setLayout(new GridLayout(2, false));
	    fRCPGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fRCPGroup.setText(PDEUIMessages.PluginContentPage_rcpGroup); 
	    
	    createRCPQuestion(fRCPGroup, 2);    
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
	    fLabel.setText(PDEUIMessages.PluginContentPage_appQuestion); 
	    fLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    
	    fYesButton = new Button(comp, SWT.RADIO);
	    fYesButton.setText(PDEUIMessages.PluginContentPage_yes); 
	    fYesButton.setSelection(false);
	    gd = new GridData();
	    gd.widthHint = getButtonWidthHint(fYesButton);
	    fYesButton.setLayoutData(gd);
	    fYesButton.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent e) {
	    		updateData();
	    		getContainer().updateButtons();
	    	}
	    });
	    
	    fNoButton = new Button(comp, SWT.RADIO);
	    fNoButton.setText(PDEUIMessages.PluginContentPage_no); 
	    fNoButton.setSelection(true);
	    gd = new GridData();
	    gd.widthHint = getButtonWidthHint(fNoButton);
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
			fUIPlugin.setEnabled(!fData.isSimple());

			// plugin class group
			if (((fChangedGroups & P_CLASS_GROUP) == 0)){
				int oldfChanged = fChangedGroups;
				presetClassField(fClassText, computeId(), "Plugin"); //$NON-NLS-1$
				fChangedGroups = oldfChanged;
			}		
			fRCPGroup.setVisible(!fData.isLegacy() && !fData.isSimple());
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
                        buffer.append(suffix.toLowerCase(Locale.ENGLISH));
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
                    buffer.append(suffix.toLowerCase(Locale.ENGLISH));
				buffer.append("." + Character.toUpperCase(token.charAt(0)) + token.substring(1) + suffix); //$NON-NLS-1$ 
            }
		}
		text.setText(buffer.toString());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#validatePage()
	 */
	protected void validatePage() {
		String errorMessage = validateProperties();
		if (errorMessage == null && !fData.isSimple()) {
			if (fLibraryText.getText().trim().length() == 0 && fData.getTargetVersion().equals(ICoreConstants.TARGET21)) {
				errorMessage = PDEUIMessages.PluginContentPage_noLibrary; 
			}	
		}
		if (errorMessage == null && fGenerateClass.isEnabled() && fGenerateClass.getSelection()) {
			IStatus status = JavaConventions.validateJavaTypeName(fClassText.getText().trim());
			if (status.getSeverity() == IStatus.ERROR) {
				errorMessage = status.getMessage();
			} else if (status.getSeverity() == IStatus.WARNING) {
				setMessage(status.getMessage(), DialogPage.WARNING);
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.plugin.ContentPage#getNameFieldQualifier()
	 */
	protected String getNameFieldQualifier() {
		return PDEUIMessages.ContentPage_plugin; 
	}
	
	private static int getButtonWidthHint(Button button) {
		if (button.getFont().equals(JFaceResources.getDefaultFont()))
			button.setFont(JFaceResources.getDialogFont());
		return Math.max(50,
				button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}
}
