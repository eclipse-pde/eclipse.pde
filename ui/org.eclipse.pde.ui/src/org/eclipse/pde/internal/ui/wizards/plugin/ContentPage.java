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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class ContentPage extends WizardPage {

	protected boolean fInitialized = false;
	protected Text fIdText;
	protected Text fVersionText;
	protected Text fNameText;
	protected Text fProviderText;
	protected Label fLibraryLabel;
	protected Text fLibraryText;

	protected NewProjectCreationPage fMainPage;
	protected AbstractFieldData fData;
	protected IProjectProvider fProjectProvider;

	protected final static int PROPERTIES_GROUP = 1;
	
	protected int fChangedGroups = 0;
	
	protected ModifyListener propertiesListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (fInitialized)
				fChangedGroups |= PROPERTIES_GROUP;
			validatePage();
		}
	};
	public ContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName);
		fMainPage = page;
		fProjectProvider = provider;
		fData = data;
	}

	protected Text createText(Composite parent, ModifyListener listener) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(listener);
		return text;
	}
	
	protected abstract void validatePage();

	protected String validateProperties() {
		if (!fInitialized) {
			if (!fIdText.getText().trim().equals(fProjectProvider.getProjectName())) 
				setMessage(PDEUIMessages.ContentPage_illegalCharactersInID, INFORMATION);
			else
				setMessage(null);
			return null;
		}
		
		setMessage(null);
		String errorMessage = validateId();
		if (errorMessage != null)
			return errorMessage;
		
		if (fVersionText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.ContentPage_noversion; 
		} else if (!isVersionValid(fVersionText.getText().trim())) {
			errorMessage = PDEUIMessages.ContentPage_badversion; 
		} else if (fNameText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.ContentPage_noname; 
		}
		
		return errorMessage;
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.ContentPage_noid; 

		if (!IdUtil.isValidCompositeID(id)) {
			return PDEUIMessages.ContentPage_invalidId; 
		}
		return null;
	}

	protected boolean isVersionValid(String version) {
		try {
			new PluginVersionIdentifier(version);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		updateData();
		return super.getNextPage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {		
			// update the library field/label enabled state
			fLibraryLabel.setEnabled(!fData.isSimple());
			fLibraryText.setEnabled(!fData.isSimple());

			String id = computeId();
			// properties group
			if ((fChangedGroups & PROPERTIES_GROUP) == 0) {
				int oldfChanged = fChangedGroups;				
				fIdText.setText(id);
				fVersionText.setText("1.0.0"); //$NON-NLS-1$
				fNameText.setText(IdUtil.getValidName(id, getNameFieldQualifier()));
				fProviderText.setText(IdUtil.getValidProvider(id));
				presetLibraryField(id);
				fChangedGroups = oldfChanged;
			}
			if (fInitialized)
				validatePage();
			else
				fInitialized = true;
		} 
		super.setVisible(visible);
	}
	
	protected String computeId() {
		return IdUtil.getValidId(fProjectProvider.getProjectName());
	}

	private void presetLibraryField(String id){
		double version = Double.parseDouble(fData.getTargetVersion());
		if (version >= 3.1) {
			fLibraryText.setText(""); //$NON-NLS-1$
			return;
		}
		
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens())
				fLibraryText.setText(token + ".jar"); //$NON-NLS-1$
		}
	}
	
	protected abstract String getNameFieldQualifier();

	public void updateData() {
		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		if (!fData.isSimple()) {
			String library = fLibraryText.getText().trim();
			if (library.length() > 0) {			
				if (!library.endsWith(".jar") &&!library.endsWith("/") && !library.equals(".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					library += "/"; //$NON-NLS-1$
				fData.setLibraryName(library);
			} else {
				fData.setLibraryName(null);
			}
		}
	}

	public IFieldData getData() {
		return fData;
	}

	public String getId() {
		return fIdText.getText().trim();
	}
}
