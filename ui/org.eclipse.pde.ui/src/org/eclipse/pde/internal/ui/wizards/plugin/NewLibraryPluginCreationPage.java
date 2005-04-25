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

import java.util.StringTokenizer;

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
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

	protected Button fBundleCheck;

	private AbstractFieldData fData;

	protected Text fIdText;

	protected Button fJarredCheck;

	protected NewLibraryPluginCreationPage fMainPage;

	protected Text fNameText;

	protected IProjectProvider fProjectProvider;

	protected PropertiesListener fPropertiesListener = new PropertiesListener();

	protected Text fProviderText;

	private Combo fTargetCombo;

	protected Text fVersionText;

	public NewLibraryPluginCreationPage(String pageName, AbstractFieldData data) {
		super(pageName);
		fData = data;
		setTitle(PDEUIMessages.NewLibraryPluginCreationPage_title); //$NON-NLS-1$
		setDescription(PDEUIMessages.NewLibraryPluginCreationPage_desc); //$NON-NLS-1$
	}

	protected String computeId() {
		return getProjectName().replaceAll("[^a-zA-Z0-9\\._]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		control.setLayout(layout);

		createPluginPropertiesGroup(control);

		createFormatGroup(control);

		Dialog.applyDialogFont(control);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control,
				IHelpContextIds.NEW_LIBRARY_PROJECT_STRUCTURE_PAGE);
		setControl(control);
	}

	private void createFormatGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.NewLibraryPluginCreationPage_pformat); //$NON-NLS-1$			
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pTarget); //$NON-NLS-1$
		fTargetCombo = new Combo(group, SWT.READ_ONLY | SWT.SINGLE);
		fTargetCombo.setItems(new String[] { ICoreConstants.TARGET31,
				ICoreConstants.TARGET30, ICoreConstants.TARGET21 });
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		fTargetCombo.setLayoutData(gd);
		fTargetCombo.setText(PDECore.getDefault().getTargetVersion());
		fTargetCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateBundleCheck();
			}
		});
		fBundleCheck = new Button(group, SWT.CHECK);
		fBundleCheck.setText(PDEUIMessages.NewLibraryPluginCreationPage_bundle); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fBundleCheck.setLayoutData(gd);
		fJarredCheck = new Button(group, SWT.CHECK);
		fJarredCheck.setText(PDEUIMessages.NewLibraryPluginCreationPage_jarred); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fJarredCheck.setLayoutData(gd);
		updateBundleCheck();
	}

	private void createPluginPropertiesGroup(Composite container) {
		Group propertiesGroup = new Group(container, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(2, false));
		propertiesGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		propertiesGroup.setText(PDEUIMessages.NewLibraryPluginCreationPage_pGroup); //$NON-NLS-1$

		Label label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pid); //$NON-NLS-1$
		fIdText = createText(propertiesGroup, fPropertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pversion); //$NON-NLS-1$
		fVersionText = createText(propertiesGroup, fPropertiesListener);
		fPropertiesListener.setBlocked(true);
		fVersionText.setText("1.0.0"); //$NON-NLS-1$
		fPropertiesListener.setBlocked(false);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pname); //$NON-NLS-1$
		fNameText = createText(propertiesGroup, fPropertiesListener);

		label = new Label(propertiesGroup, SWT.NONE);
		label.setText(PDEUIMessages.NewLibraryPluginCreationPage_pprovider); //$NON-NLS-1$
		fProviderText = createText(propertiesGroup, fPropertiesListener);

	}

	protected Text createText(Composite parent, ModifyListener listener) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(listener);
		return text;
	}

	public String getId() {
		return fIdText.getText().trim();
	}

	protected String getNameFieldQualifier() {
		return PDEUIMessages.NewLibraryPluginCreationPage_plugin; //$NON-NLS-1$
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

	public boolean hasBundleStructure() {
		if (fBundleCheck == null)
			return false;
		return fBundleCheck.getSelection();
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

	private void updateBundleCheck() {
		boolean legacy = fTargetCombo.getText().equals(ICoreConstants.TARGET21);
		fBundleCheck.setEnabled(!legacy);
		fBundleCheck.setSelection(!legacy);
		fJarredCheck.setEnabled(!legacy);
		boolean pre31 = fTargetCombo.getText().equals(ICoreConstants.TARGET30)
				|| fTargetCombo.getText().equals(ICoreConstants.TARGET21);
		fJarredCheck.setSelection(!pre31);
	}

	public void updateData() {
		fData.setSimple(false);
		fData.setSourceFolderName(null);
		fData.setOutputFolderName(null);
		fData.setHasBundleStructure(fBundleCheck.isEnabled()
				&& fBundleCheck.getSelection());
		fData.setLegacy(fTargetCombo.getText().equals("2.1")); //$NON-NLS-1$
		fData.setTargetVersion(fTargetCombo.getText());

		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderText.getText().trim());
		fData.setLibraryName(null);
		fData.setJarred(fJarredCheck.isEnabled()
				&& fJarredCheck.getSelection());

		PluginFieldData data = (PluginFieldData) fData;
		data.setClassname(null);
		data.setUIPlugin(false);
		data.setDoGenerateClass(false);
		data.setRCPApplicationPlugin(false);
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.NewLibraryPluginCreationPage_noid; //$NON-NLS-1$

		if (!IdUtil.isValidPluginId(id)) { //$NON-NLS-1$
			return PDEUIMessages.NewLibraryPluginCreationPage_invalidId; //$NON-NLS-1$
		}
		return null;
	}

	protected boolean validatePage() {
		String id = computeId();
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
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noversion; //$NON-NLS-1$
		} else if (!isVersionValid(fVersionText.getText().trim())) {
			errorMessage = PDEUIMessages.ContentPage_badversion; //$NON-NLS-1$
		} else if (fNameText.getText().trim().length() == 0) {
			errorMessage = PDEUIMessages.NewLibraryPluginCreationPage_noname; //$NON-NLS-1$
		}

		if (errorMessage != null)
			return errorMessage;

		return errorMessage;
	}
}
