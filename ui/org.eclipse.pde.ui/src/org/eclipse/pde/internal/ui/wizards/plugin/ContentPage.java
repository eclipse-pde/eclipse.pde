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
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author melhem
 *  
 */
public abstract class ContentPage extends WizardPage {

	protected boolean fIsFragment;
	protected boolean isInitialized = false;
	protected Text fIdText;
	protected Text fVersionText;
	protected Text fNameText;
	protected Text fProviderText;
	protected Text fPluginIdText;
	protected Text fPluginVersion;
	protected Combo fMatchCombo;
	protected Button fLegacyButton;
	protected AbstractFieldData fData;
	protected IProjectProvider fProjectProvider;
	protected Label fLibraryLabel;
	protected Text fLibraryText;
	
	protected final static int PROPERTIES_GROUP = 1;
	protected final static int P_CLASS_GROUP = 2;
	protected int fChangedGroups = 0;
	protected ModifyListener listener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};
	
	protected ModifyListener propertiesListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (isInitialized)
				fChangedGroups |= PROPERTIES_GROUP;
			validatePage();
		}
	};
	protected static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	protected static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	protected static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	protected static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater"; //$NON-NLS-1$
	protected Text fClassText;
	protected Button fGenerateClass;
	protected Button fUIPlugin;
	protected Label fClassLabel;
	protected NewProjectCreationPage creationPage;

	
	public ContentPage(String pageName, IProjectProvider provider,
			NewProjectCreationPage page, AbstractFieldData data, boolean isFragment) {
		super(pageName);
		creationPage = page;
		fIsFragment = isFragment;
		fProjectProvider = provider;
		fData = data;
		if (isFragment) {
			setTitle(PDEPlugin.getResourceString("ContentPage.ftitle")); //$NON-NLS-1$
			setDescription(PDEPlugin.getResourceString("ContentPage.fdesc")); //$NON-NLS-1$
		} else {
			setTitle(PDEPlugin.getResourceString("ContentPage.title")); //$NON-NLS-1$
			setDescription(PDEPlugin.getResourceString("ContentPage.desc")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		createPropertyControls(container);
		fLegacyButton = new Button(container, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fLegacyButton.setLayoutData(gd);
		fLegacyButton.setText(PDEPlugin.getResourceString("ContentPage.legacy")); //$NON-NLS-1$
		fLegacyButton.setSelection(!PDECore.getDefault().getModelManager()
				.isOSGiRuntime());
		fLegacyButton.addSelectionListener(new SelectionAdapter(){
		    /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                updateBranding(fLegacyButton.getSelection());
            }
		});
		Dialog.applyDialogFont(container);
		setControl(container);
	}

	protected abstract void createPropertyControls(Composite container);

	protected Text createText(Composite parent, ModifyListener listener) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(listener);
		return text;
	}

	protected void validatePage() {
		setMessage(null);
		String errorMessage = validateId();
		if (errorMessage == null) {
			if (fVersionText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.noversion"); //$NON-NLS-1$
			} else if (!isVersionValid(fVersionText.getText().trim())) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.badversion"); //$NON-NLS-1$
			} else if (fNameText.getText().trim().length() == 0) {
				errorMessage = PDEPlugin.getResourceString("ContentPage.noname"); //$NON-NLS-1$
			}
		}
		if (errorMessage == null) {
			if (creationPage.isJavaProject()
					&& fLibraryText.getText().trim().length() == 0)
				errorMessage = PDEPlugin
						.getResourceString("ProjectStructurePage.noLibrary"); //$NON-NLS-1$

			if (fIsFragment) {
				String pluginID = fPluginIdText.getText().trim();
				if (pluginID.length() == 0) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.nopid"); //$NON-NLS-1$
				} else if (PDECore.getDefault().getModelManager().findEntry(pluginID) == null) {
					errorMessage = PDEPlugin
							.getResourceString("ContentPage.pluginNotFound"); //$NON-NLS-1$
				} else if (fPluginVersion.getText().trim().length() == 0) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.nopversion"); //$NON-NLS-1$
				} else if (!isVersionValid(fPluginVersion.getText().trim())) {
					errorMessage = PDEPlugin.getResourceString("ContentPage.badpversion"); //$NON-NLS-1$
				}
			} else if (fGenerateClass.isEnabled() && fGenerateClass.getSelection()) {
				IStatus status = JavaConventions.validateJavaTypeName(fClassText
						.getText().trim());
				if (status.getSeverity() == IStatus.ERROR) {
					errorMessage = status.getMessage();
				} else if (status.getSeverity() == IStatus.WARNING) {
					setMessage(status.getMessage(), DialogPage.WARNING);
				}
			}
		}
		
		if (isInitialized)
			setErrorMessage(errorMessage);
		else
			setErrorMessage(null);
		setPageComplete(errorMessage == null);
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEPlugin.getResourceString("ContentPage.noid"); //$NON-NLS-1$

		StringTokenizer stok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (!Character.isLetterOrDigit(token.charAt(i)) && '_' != token.charAt(i))
					return PDEPlugin.getResourceString("ContentPage.invalidId"); //$NON-NLS-1$
			}
		}
		return null;
	}

	protected abstract void updateBranding(boolean isLegacy);
	public abstract boolean isRCPApplication();
	
	private boolean isVersionValid(String version) {
		try {
			new PluginVersionIdentifier(version);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {

		if (visible) {
			if (creationPage.hasBundleStructure() || isRCPApplication()) {
				fLegacyButton.setEnabled(false);
			} else {
				fLegacyButton.setEnabled(true);
			}
			
			fLibraryLabel.setEnabled(creationPage.isJavaProject());
			fLibraryText.setEnabled(creationPage.isJavaProject());

			if (!fIsFragment) {
				if (!creationPage.isJavaProject()) {
					fGenerateClass.setEnabled(false);
					fClassLabel.setEnabled(false);
					fClassText.setEnabled(false);
					fUIPlugin.setEnabled(false);
				} else {
					fGenerateClass.setEnabled(true);
					if (fGenerateClass.getSelection()){
						fClassLabel.setEnabled(true);
						fClassText.setEnabled(true);
						fUIPlugin.setEnabled(true);
					}
				}
			}
		}
		
		if (visible){
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
			// plugin class group
			if (!fIsFragment && ((fChangedGroups & P_CLASS_GROUP) == 0)){
				int oldfChanged = fChangedGroups;
				presetClassField(id);
				fChangedGroups = oldfChanged;
			}
			if (isInitialized)
				validatePage();
			isInitialized = true;
		} else
			updateData();
		super.setVisible(visible);
	}

	private String computeId() {
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
				fNameText
						.setText(Character.toUpperCase(token.charAt(0))
								+ ((token.length() > 1) ? token.substring(1) : "") //$NON-NLS-1$
								+ " " + (fIsFragment ? PDEPlugin.getResourceString("ContentPage.fragment") : PDEPlugin.getResourceString("ContentPage.plugin"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	private void presetProviderField(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		int count = tok.countTokens();
		if (count > 2 && tok.nextToken().equals("com")) //$NON-NLS-1$
			fProviderText.setText(tok.nextToken().toUpperCase());
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
				buffer
						.append("." + Character.toUpperCase(token.charAt(0)) + token.substring(1) + "Plugin"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fClassText.setText(buffer.toString());
	}

	public void updateData() {
		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		fData.setIsLegacy(fLegacyButton.isEnabled() && fLegacyButton.getSelection());
		if (creationPage.isJavaProject()) {
			String library = fLibraryText.getText().trim();
			if (!library.endsWith(".jar") &&!library.endsWith("/") && !library.equals(".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					library += "/"; //$NON-NLS-1$
			fData.setLibraryName(library);
		}
	}

	public IFieldData getData() {
		return fData;
	}

	public String getId() {
		return fIdText.getText().trim();
	}
}
