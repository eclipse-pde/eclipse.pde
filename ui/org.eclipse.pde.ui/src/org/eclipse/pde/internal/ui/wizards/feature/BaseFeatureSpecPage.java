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

package org.eclipse.pde.internal.ui.wizards.feature;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
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
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public abstract class BaseFeatureSpecPage extends WizardPage {
	
	private boolean isPatch;
	protected WizardNewProjectCreationPage mainPage;
	protected Text featureIdText;
	protected Text featureNameText;
	protected Text featureVersionText;
	protected Text featureProviderText;
	protected Text patchIdText;
	protected Text patchNameText;
	protected Text patchProviderText;
	protected Text libraryText;
	protected Button browseButton;
	protected Button customChoice;
	protected String initialId;
	protected String initialName;
	protected Label libraryLabel;
	protected boolean isInitialized = false;
	protected IFeatureModel fFeatureToPatch;
	
	public BaseFeatureSpecPage(WizardNewProjectCreationPage mainPage,
			boolean isPatch) {
		super("specPage"); //$NON-NLS-1$
		this.isPatch = isPatch;
		this.mainPage = mainPage;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 12;
		layout.horizontalSpacing = 9;
		container.setLayout(layout);
		
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		};
		
		if (isPatch()) {
			Group patchPropertiesGroup = new Group(container, SWT.NULL);
			layout = new GridLayout(2, false);
			patchPropertiesGroup.setLayout(layout);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			patchPropertiesGroup.setLayoutData(gd);
			patchPropertiesGroup.setText(PDEUIMessages.NewFeatureWizard_SpecPage_patchProperties); //$NON-NLS-1$
			Label label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_id);
			patchIdText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchIdText.setLayoutData(gd);
			if (initialId != null)
				patchIdText.setText(initialId);
			patchIdText.addModifyListener(listener);
			
			label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_name);
			patchNameText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchNameText.setLayoutData(gd);
			if (initialName != null)
				patchNameText.setText(initialName);
			patchNameText.addModifyListener(listener);
			
			label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_provider);
			patchProviderText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchProviderText.setLayoutData(gd);
			patchProviderText.addModifyListener(listener);
		}
		addFeatureProperties(container, listener);
		addCustomInstallHandlerSection(container, listener);
		
		setControl(container);
		Dialog.applyDialogFont(container);
	}
	private void addCustomInstallHandlerSection(Composite parent, ModifyListener listener) {
		Group customHandlerGroup = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		customHandlerGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		customHandlerGroup.setLayoutData(gd);
		customHandlerGroup.setText(PDEUIMessages.BaseFeatureSpecPage_customGroup); //$NON-NLS-1$

		customChoice = new Button(customHandlerGroup, SWT.CHECK);
		if (!isPatch())
			customChoice.setText(PDEUIMessages.NewFeatureWizard_SpecPage_customProject);
		else 
			customChoice.setText(PDEUIMessages.NewFeatureWizard_SpecPage_patch_customProject);
		customChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.widget).getSelection();
				libraryText.setEnabled(isSelected);
				libraryLabel.setEnabled(isSelected);
				verifyComplete();
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		customChoice.setLayoutData(gd);
		
		libraryLabel = new Label(customHandlerGroup, SWT.NULL);
		libraryLabel.setText(
			PDEUIMessages.NewFeatureWizard_SpecPage_library);
		gd = new GridData();
		gd.horizontalIndent = 22;
		libraryLabel.setLayoutData(gd);
		libraryText = new Text(customHandlerGroup, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		libraryText.addModifyListener(listener);
		
	}
	public boolean isPatch() {
		return isPatch;
	}
	
	protected abstract void verifyComplete();
	/**
	 * @return Returns the initialName.
	 */
	public String getInitialName() {
		return initialName;
	}
	
	/**
	 * @param initialName
	 *            The initialName to set.
	 */
	public void setInitialName(String initialName) {
		this.initialName = initialName;
	}
	
	/**
	 * 
	 * @param initialId
	 */
	public void setInitialId(String initialId) {
		this.initialId = initialId;
	}
	
	/**
	 * @return Returns the initialId.
	 */
	public String getInitialId() {
		return initialId;
	}
	
	protected void initialize(){
		customChoice.setSelection(false);
		libraryText.setEnabled(false);
		libraryLabel.setEnabled(false);
	}
	
	private void addFeatureProperties(Composite container, ModifyListener listener){
		Group featurePropertiesGroup = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		featurePropertiesGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		featurePropertiesGroup.setLayoutData(gd);
		
		if (isPatch()){
			featurePropertiesGroup.setText(PDEUIMessages.BaseFeatureSpecPage_patchGroup_title); //$NON-NLS-1$
						
			Label label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_id);
			
			Composite patchcontainer = new Composite(featurePropertiesGroup, SWT.NULL);
			layout = new GridLayout(2, false);
			layout.marginHeight = layout.marginWidth =0;
			layout.horizontalSpacing = 5;
			patchcontainer.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			patchcontainer.setLayoutData(gd);
			
			featureIdText = new Text(patchcontainer, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
			browseButton = new Button(patchcontainer, SWT.PUSH);
			browseButton.setText(PDEUIMessages.BaseFeatureSpecPage_browse); //$NON-NLS-1$
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			browseButton.setLayoutData(gd);
			browseButton.addSelectionListener(new SelectionAdapter() {
				
				public void widgetSelected(SelectionEvent e) {
					FeatureSelectionDialog dialog = new FeatureSelectionDialog(
							getShell(), PDECore.getDefault()
									.getFeatureModelManager().getModels(),
							false);
					dialog.create();
					if (dialog.open() == Window.OK) {
						Object[] result = dialog.getResult();
						IFeatureModel selectedModel = (IFeatureModel) result[0];
						featureIdText.setText(selectedModel.getFeature().getId());
						featureNameText.setText(selectedModel.getFeature().getLabel());
						featureVersionText.setText(selectedModel.getFeature().getVersion());
						fFeatureToPatch = selectedModel;
					}
				}
			});
			SWTUtil.setButtonDimensionHint(browseButton);
		} else {
			featurePropertiesGroup.setText(PDEUIMessages.BaseFeatureSpecPage_featurePropertiesGroup_title); //$NON-NLS-1$
			
			Label label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_id);
			featureIdText = new Text(featurePropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
		}
		
		Label label = new Label(featurePropertiesGroup, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_name);
		featureNameText = new Text(featurePropertiesGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureNameText.setLayoutData(gd);
		if (initialName != null)
			featureNameText.setText(initialName);
		featureNameText.addModifyListener(listener);
		
		label = new Label(featurePropertiesGroup, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_version);
		featureVersionText = new Text(featurePropertiesGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureVersionText.setLayoutData(gd);
		featureVersionText.addModifyListener(listener);
		if (!isPatch()) {
			label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_provider);
			featureProviderText = new Text(featurePropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureProviderText.setLayoutData(gd);
			featureProviderText.addModifyListener(listener);
		}
	}
	protected String computeInitialId(String projectName) {
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
	
	protected String verifyVersion() {
		String problemText = PDEUIMessages.NewFeatureWizard_SpecPage_versionFormat;
		String value = featureVersionText.getText();
		if (PluginVersionIdentifier.validateVersion(value).getSeverity() != IStatus.OK)
			return problemText;
		return null;
	}
	
	protected String verifyIdRules() {
		String problemText = PDEUIMessages.NewFeatureWizard_SpecPage_invalidId;
		String name = featureIdText.getText();
		if (name == null || name.length() == 0)
			 return PDEUIMessages.NewFeatureWizard_SpecPage_missing;
		StringTokenizer stok = new StringTokenizer(name, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)) == false)
					return problemText;
			}
		}
		return null;
	}
	
	public IFeatureModel getFeatureToPatch(){
		return fFeatureToPatch;
	}
		
	protected String getInstallHandlerLibrary() {
		if (!customChoice.getSelection())
			return null;
		String library = libraryText.getText();
		if (!library.endsWith(".jar") && !library.endsWith("/") && !library.equals(".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			library += "/"; //$NON-NLS-1$
		return library;
	}
}
