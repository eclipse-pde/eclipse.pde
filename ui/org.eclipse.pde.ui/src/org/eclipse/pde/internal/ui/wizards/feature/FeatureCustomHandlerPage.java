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
package org.eclipse.pde.internal.ui.wizards.feature;

import java.util.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class FeatureCustomHandlerPage extends WizardPage {
	private static final String KEY_TITLE = "FeatureCustomHandlerPage.title"; //$NON-NLS-1$
	private static final String KEY_OUTPUT = "ProjectStructurePage.output"; //$NON-NLS-1$
	private static final String KEY_LIBRARY = "FeatureCustomHandlerPage.library"; //$NON-NLS-1$
	private static final String KEY_SOURCE = "ProjectStructurePage.source"; //$NON-NLS-1$
	private static final String KEY_DESC = "FeatureCustomHandlerPage.desc"; //$NON-NLS-1$
	private static final String KEY_CUSTOM_INSTALL_HANDLER =
		"FeatureCustomHandlerPage.customProject"; //$NON-NLS-1$
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle"; //$NON-NLS-1$
	public static final String FEATURE_LIBRARY_ERR = "FeatureCustomHandlerPage.error.library"; //$NON-NLS-1$
	public static final String FEATURE_SOURCE_ERR = "FeatureCustomHandlerPage.error.source"; //$NON-NLS-1$
	public static final String FEATURE_OUTPUT_ERR = "FeatureCustomHandlerPage.error.output"; //$NON-NLS-1$

	private IProjectProvider provider;
	private Text buildOutputText;
	private Text sourceText;
	private Text libraryText;
	private Button customChoice;
	private StructureData data;
	private Label libraryLabel;
	private Label sourceLabel;
	private Label buildOutputLabel;
	private boolean isInitialized = false;

	class StructureData {
		String buildOutput;
		String library;
		String source;
		boolean hasCustomHandler;

		public String getJavaBuildFolderName() {
			return buildOutput;
		}
		public String getSourceFolderName() {
			return source;
		}
		public String getRuntimeLibraryName() {
			if (library != null && !library.endsWith(".jar")) //$NON-NLS-1$
				library += ".jar"; //$NON-NLS-1$
			return library;
		}
		public boolean hasCustomHandler(){
			return hasCustomHandler;
		}
	}

	public FeatureCustomHandlerPage(IProjectProvider provider) {
		super("projectStructure"); //$NON-NLS-1$
		this.provider = provider;
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	
	private void addCustomInstallHandlerSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		customChoice = new Button(container, SWT.CHECK);
		customChoice.setText(PDEPlugin.getResourceString(KEY_CUSTOM_INSTALL_HANDLER));
		customChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.widget).getSelection();
				libraryText.setEnabled(isSelected);
				sourceText.setEnabled(isSelected);
				buildOutputText.setEnabled(isSelected);
				libraryLabel.setEnabled(isSelected);
				sourceLabel.setEnabled(isSelected);
				buildOutputLabel.setEnabled(isSelected);
				getContainer().updateButtons();
			}
		});
	}

	private void addCustomInstallHandlerPropertiesSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		libraryLabel = new Label(container, SWT.NULL);
		libraryLabel.setText(
			PDEPlugin.getResourceString(KEY_LIBRARY));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		libraryLabel.setLayoutData(gd);
		libraryText = new Text(container, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		libraryText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				if (libraryText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}	
		});

		sourceLabel = new Label(container, SWT.NULL);
		sourceLabel.setText(PDEPlugin.getResourceString(KEY_SOURCE));
		gd = new GridData();
		gd.horizontalIndent = 25;
		sourceLabel.setLayoutData(gd);
		sourceText = new Text(container, SWT.SINGLE | SWT.BORDER);
		sourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sourceText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				if (sourceText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}
		});
		
		buildOutputLabel = new Label(container, SWT.NULL);
		buildOutputLabel.setText(PDEPlugin.getResourceString(KEY_OUTPUT));
		gd = new GridData();
		gd.horizontalIndent = 25;
		buildOutputLabel.setLayoutData(gd);
		buildOutputText = new Text(container, SWT.SINGLE | SWT.BORDER);
		buildOutputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		buildOutputText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				if (buildOutputText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}
		});
	}
	
	private void evalErrMsg(){
		if (!customChoice.getSelection())
			return;
		if (libraryText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_LIBRARY_ERR));
		else if (sourceText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_SOURCE_ERR));
		else if (buildOutputText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_OUTPUT_ERR));
		else{
			setErrorMessage(null);
			setPageComplete(true);
		}
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		addCustomInstallHandlerSection(container);
		addCustomInstallHandlerPropertiesSection(container);

		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
	}


	public StructureData getStructureData() {
		data = new StructureData();
		data.buildOutput =
			(!customChoice.getSelection()) ? null : buildOutputText.getText();
		data.library =
			(!customChoice.getSelection()) ? null : libraryText.getText();
		data.source =
			(!customChoice.getSelection()) ? null : sourceText.getText();
		data.hasCustomHandler = customChoice.getSelection();
		return data;
	}

	public boolean isInitialized(){
		return isInitialized;
	}
	private void initialize() {
		if (isInitialized)
			return;
		customChoice.setSelection(false);
		if (buildOutputText.getText().equals("")) //$NON-NLS-1$
			buildOutputText.setText("bin"); //$NON-NLS-1$
		if (libraryText.getText().equals("") //$NON-NLS-1$
			|| libraryText.getText().equals(".jar")) { //$NON-NLS-1$
			String lastSegment = setInitialId(provider.getProjectName());
			int loc = lastSegment.lastIndexOf('.');
			if (loc != -1) {
				lastSegment = lastSegment.substring(loc + 1);
			}
			libraryText.setText(lastSegment + ".jar"); //$NON-NLS-1$
		}
		if (sourceText.getText().equals("")) //$NON-NLS-1$
			sourceText.setText("src"); //$NON-NLS-1$
		if (customChoice != null){
			libraryText.setEnabled(customChoice.getSelection());
			sourceText.setEnabled(customChoice.getSelection());
			buildOutputText.setEnabled(customChoice.getSelection());
			libraryLabel.setEnabled(customChoice.getSelection());
			sourceLabel.setEnabled(customChoice.getSelection());
			buildOutputLabel.setEnabled(customChoice.getSelection());
		}

	}
	public boolean isPageComplete() {
		if (!customChoice.getSelection())
			return true;
		// java choice selected
		return (libraryText.getText().length() > 0);
	}

	private String setInitialId(String projectName) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stok = new StringTokenizer(projectName, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)))
					buffer.append(token.charAt(i));
			}
			if (stok.hasMoreTokens()
				&& buffer.charAt(buffer.length() - 1) != '.')
				buffer.append("."); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			isInitialized=true;
		}
	}
}
