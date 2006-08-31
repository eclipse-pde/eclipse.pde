/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class ExportOptionsTab extends AbstractExportTab {

	protected static final String S_JAR_FORMAT = "exportUpdate"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE="exportSource"; //$NON-NLS-1$
	private static final String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private static final String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$
	private static final String S_QUALIFIER = "qualifier"; //$NON-NLS-1$
	private static final String S_QUALIFIER_NAME = "qualifierName"; //$NON-NLS-1$
		
	private Button fIncludeSource;
	protected Button fJarButton;
	private Button fSaveAsAntButton;
	private Combo fAntCombo;
	private Button fBrowseAnt;
	
	private Button fQualifierButton;	
	private Text fQualifierText;

	public ExportOptionsTab(BaseExportWizardPage page) {
		super(page);
	}

	protected Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		addSourceOption(container);
		addJAROption(container);
		addCrossPlatformOption(container);
		addAntSection(container);
		addQualifierOption(container);
									
 		return container;
	}
	
	protected void addSourceOption(Composite comp) {
		fIncludeSource = new Button(comp, SWT.CHECK);
		fIncludeSource.setText(PDEUIMessages.ExportWizard_includeSource); 
	}
	
	protected void addJAROption(Composite comp) {
		fJarButton = new Button(comp, SWT.CHECK);
		fJarButton.setText(getJarButtonText());
		fJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			}
		});	
	}
	
	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_packageJARs; 
	}

	protected void addCrossPlatformOption(Composite comp) {
	}
	
	protected void addAntSection(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fSaveAsAntButton = new Button(comp, SWT.CHECK);
		fSaveAsAntButton.setText(PDEUIMessages.ExportWizard_antCheck); 
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		fSaveAsAntButton.setLayoutData(gd);
		
		fAntCombo = new Combo(comp, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 15;
		fAntCombo.setLayoutData(gd);
		
		fBrowseAnt = new Button(comp, SWT.PUSH);
		fBrowseAnt.setText(PDEUIMessages.ExportWizard_browse); 
		fBrowseAnt.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseAnt);		
	}
	
	protected void addQualifierOption(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fQualifierButton = new Button(comp, SWT.CHECK);
		fQualifierButton.setText(PDEUIMessages.AdvancedPluginExportPage_qualifier);		
		
		fQualifierText = new Text(comp, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 15;
		fQualifierText.setLayoutData(gd);
		fQualifierText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fPage.pageChanged();
			}
		});
	}

	protected void initialize(IDialogSettings settings) {
		fIncludeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
        fJarButton.setSelection(getInitialJarButtonSelection(settings));
		fSaveAsAntButton.setSelection(settings.getBoolean(S_SAVE_AS_ANT));
		initializeCombo(settings, S_ANT_FILENAME, fAntCombo);
		fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
		fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
		fQualifierButton.setSelection(settings.getBoolean(S_QUALIFIER));
		fQualifierText.setText(getInitialQualifierText(settings));
		fQualifierText.setEnabled(fQualifierButton.getSelection());
		hookListeners();
	}

	protected void saveSettings(IDialogSettings settings) {
        settings.put(S_JAR_FORMAT, fJarButton.getSelection());
		settings.put(S_EXPORT_SOURCE, fIncludeSource.getSelection());
        settings.put(S_SAVE_AS_ANT, fSaveAsAntButton.getSelection());
        settings.put(S_QUALIFIER, fQualifierButton.getSelection());
        settings.put(S_QUALIFIER_NAME, fQualifierText.getText());
        saveCombo(settings, S_ANT_FILENAME, fAntCombo);
	}
	
	private String getInitialQualifierText(IDialogSettings settings) {
		String qualifier = settings.get(S_QUALIFIER_NAME);
		if(qualifier == null || qualifier.equals("")) //$NON-NLS-1$
			return FeatureExportOperation.getDate(); 
		return qualifier;
	}
	
	protected boolean getInitialJarButtonSelection(IDialogSettings settings){
		String selected = settings.get(S_JAR_FORMAT);
		return selected == null
					? TargetPlatform.getTargetVersion() >= 3.1
					: "true".equals(selected); //$NON-NLS-1$
	}
	
	protected void hookListeners() {
    	fJarButton.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			((BaseExportWizardPage)fPage).adjustAdvancedTabsVisibility();
    		}
    	});
        fSaveAsAntButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
                fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
                fPage.pageChanged();
            }}
        );
        
 		fBrowseAnt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(fAntCombo, "*.xml"); //$NON-NLS-1$
			}
		});
		
		fAntCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPage.pageChanged();
			}
		});
		
		fAntCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fPage.pageChanged();
			}
		});	
        fQualifierButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fQualifierText.setEnabled(fQualifierButton.getSelection());
                fPage.pageChanged();
            }}
        );
 	}
	
	protected String validate() {
		if (fSaveAsAntButton.getSelection() && fAntCombo.getText().trim().length() == 0)
			return PDEUIMessages.ExportWizard_status_noantfile;
		return null;		
	}
	
	protected String validateAntCombo() {
		String path = new Path(fAntCombo.getText()).lastSegment();
		if ("build.xml".equals(path)) //$NON-NLS-1$
			return PDEUIMessages.ExportOptionsTab_antReservedMessage;
		return null;
	}
	
	protected boolean doExportSource() {
		return fIncludeSource.getSelection();
	}
	
	protected boolean useJARFormat() {
		return fJarButton.getSelection();
	}
	
	protected boolean doGenerateAntFile() {
		return fSaveAsAntButton.getSelection();
	}

	protected String getAntBuildFileName() {
		return fSaveAsAntButton.getSelection() ? fAntCombo.getText() : null;
	}
	
	protected String getQualifier() {
		if(fQualifierText.isEnabled()) {
			String qualifier = fQualifierText.getText().trim();
			if (qualifier.length() > 0)
				return qualifier;
		}
		return null;
	}
	
}
