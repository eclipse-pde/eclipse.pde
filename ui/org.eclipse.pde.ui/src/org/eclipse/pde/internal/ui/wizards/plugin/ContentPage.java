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

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

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
	protected static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	protected static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	protected static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	protected static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater"; //$NON-NLS-1$

	
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
		String errorMessage = validateId();
		if (errorMessage != null)
			return errorMessage;
		
		if (fVersionText.getText().trim().length() == 0) {
			errorMessage = PDEPlugin.getResourceString("ContentPage.noversion"); //$NON-NLS-1$
		} else if (!isVersionValid(fVersionText.getText().trim())) {
			errorMessage = PDEPlugin.getResourceString("ContentPage.badversion"); //$NON-NLS-1$
		} else if (fNameText.getText().trim().length() == 0) {
			errorMessage = PDEPlugin.getResourceString("ContentPage.noname"); //$NON-NLS-1$
		}
		
		if (errorMessage != null)
			return errorMessage;
		
		return errorMessage;
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEPlugin.getResourceString("ContentPage.noid"); //$NON-NLS-1$

		for (int i = 0; i<id.length(); i++){
			if (!id.substring(i,i+1).matches("[a-zA-Z0-9\\._]")) //$NON-NLS-1$
				return PDEPlugin.getResourceString("ContentPage.invalidId"); //$NON-NLS-1$
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
				presetNameField(id);
				presetProviderField(id);
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
		return fProjectProvider.getProjectName().replaceAll("[^a-zA-Z0-9\\._]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void presetLibraryField(String id){
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens())
				fLibraryText.setText(token + ".jar"); //$NON-NLS-1$
		}
	}
	private void presetNameField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()) {
				fNameText.setText(Character.toUpperCase(token.charAt(0))
								+ ((token.length() > 1) ? token.substring(1) : "") //$NON-NLS-1$
								+ " " + getNameFieldQualifier()); //$NON-NLS-1$
			}
		}
	}
	
	protected abstract String getNameFieldQualifier();

	private void presetProviderField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		int count = tok.countTokens();
		if (count > 2 && tok.nextToken().equals("com")) //$NON-NLS-1$
			fProviderText.setText(tok.nextToken().toUpperCase());
	}

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
