/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.jdt.ui.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;


/**
 * @author cgwong
 */
public class NewProjectCreationPage extends WizardNewProjectCreationPage {
	protected Button fJavaButton;
	protected boolean fIsFragment;
	protected Button fBundleCheck;
	private Label fSourceLabel;
	private Text fSourceText;
	private Label fOutputlabel;
	private Text fOutputText;
//	private IProjectProvider fProjectProvider;
//	private boolean fFirstVisible = true;
	private AbstractFieldData fData;
	
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
		createBundleStructureGroup(control);
		Dialog.applyDialogFont(control);
		setControl(control);
	}
	
	private void createProjectTypeGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProjectStructurePage.settings")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	
		fJavaButton = createButton(group);
		fJavaButton.setText(PDEPlugin.getResourceString("ProjectStructurePage.java")); //$NON-NLS-1$
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
		
		fSourceLabel = createLabel(group, PDEPlugin.getResourceString("ProjectStructurePage.source")); //$NON-NLS-1$
		fSourceText = createText(group);
		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		fSourceText.setText(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
		
		fOutputlabel = createLabel(group, PDEPlugin.getResourceString("ProjectStructurePage.output")); //$NON-NLS-1$
		fOutputText = createText(group);		
		fOutputText.setText(store.getString(PreferenceConstants.SRCBIN_BINNAME));
	}
	private void createBundleStructureGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProjectStructurePage.alternateFormat")); //$NON-NLS-1$
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBundleCheck = new Button(group, SWT.CHECK);
		if (fIsFragment)
			fBundleCheck.setText(PDEPlugin.getResourceString("ProjectStructurePage.fbundle")); //$NON-NLS-1$
		else
			fBundleCheck.setText(PDEPlugin.getResourceString("ProjectStructurePage.pbundle")); //$NON-NLS-1$
		
		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString("ProjectStructurePage.note")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 250;
		gd.horizontalIndent = 22;
		label.setLayoutData(gd);		
	}
	
	private Button createButton(Composite container) {
		Button button = new Button(container, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		button.setLayoutData(gd);
		return button;		
	}
	
	public boolean hasBundleStructure(){
		if (fBundleCheck == null)
			return false;
		return fBundleCheck.getSelection();
	}

	public boolean isJavaProject(){
		if (fJavaButton == null)
			return false;
		return fJavaButton.getSelection();
	}
	
	private Label createLabel(Composite container, String text) {
		Label label = new Label(container, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 22;
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
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewProjectCreationPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible)
			updateData();
	}
	public void updateData() {
		fData.setIsSimple(!isJavaProject());
		fData.setSourceFolderName(fSourceText.getText().trim());
		fData.setOutputFolderName(fOutputText.getText().trim());
		fData.setHasBundleStructure(fBundleCheck.getSelection());
	}
	
}
