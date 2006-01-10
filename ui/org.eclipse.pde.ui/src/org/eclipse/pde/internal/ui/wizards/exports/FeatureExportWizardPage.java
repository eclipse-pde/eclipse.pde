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
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;


public class FeatureExportWizardPage extends BaseExportWizardPage {
	
	private JNLPTab fJNLPTab;

	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport", //$NON-NLS-1$
			PDEUIMessages.ExportWizard_Feature_pageBlock); 
		setTitle(PDEUIMessages.ExportWizard_Feature_pageTitle); 
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}
	
	protected boolean isValidModel(IModel model) {
		return model instanceof IFeatureModel;
	}
	
	protected void createTabs(TabFolder folder) {
		super.createTabs(folder);
		IDialogSettings settings = getDialogSettings();
		boolean useDirectory = settings.getBoolean(ExportDestinationTab.S_EXPORT_DIRECTORY);
		boolean useJARFormat = settings.getBoolean(ExportOptionsTab.S_JAR_FORMAT);
		if (useDirectory && useJARFormat)
			createJNLPTab(folder);
	}
	
	protected void initializeTabs(IDialogSettings settings) {
		super.initializeTabs(settings);
		if (fJNLPTab != null)
			fJNLPTab.initialize(settings);
	}
	
	protected void createDestinationTab(TabFolder folder) {
		fDestinationTab = new FeatureDestinationTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fDestinationTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_destination); 
	}

	protected void createOptionsTab(TabFolder folder) {
		fOptionsTab = new FeatureOptionsTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fOptionsTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_options); 		
	}
	
	private void createJNLPTab(TabFolder folder) {
		fJNLPTab = new JNLPTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fJNLPTab.createControl(folder));
		item.setText(PDEUIMessages.AdvancedFeatureExportPage_jnlp); 
	}
	
	protected IModel findModelFor(IAdaptable object) {
		IProject project = (IProject) object.getAdapter(IProject.class);
		if (project != null)
			return PDECore.getDefault().getWorkspaceModelManager().getFeatureModel(project);
		return null;
	}
	
	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fJNLPTab != null)
			fJNLPTab.saveSettings(settings);
	}
	
	protected String validateTabs() {
		String message = super.validateTabs();
		if (message == null && fTabFolder.getItemCount() > 3)
			message = fJNLPTab.validate();
		return message;
	}
	
	protected void adjustAdvancedTabsVisibility() {
		adjustJARSigningTabVisibility();
		adjustJNLPTabVisibility();
		pageChanged();
	}
	
	protected void adjustJNLPTabVisibility() {
		IDialogSettings settings = getDialogSettings();
		if (useJARFormat() && doExportToDirectory()) {
			if (fTabFolder.getItemCount() < 4) {
				createJNLPTab(fTabFolder);
				fJNLPTab.initialize(settings);
			}
		} else {
			int count = fTabFolder.getItemCount();
			if (count >= 3) {
				fJNLPTab.saveSettings(settings);
				fTabFolder.getItem(count-1).dispose();
			}			
		}
	}
	
	protected boolean doMultiPlatform() {
		return ((FeatureOptionsTab)fOptionsTab).doMultiplePlatform();
	}
	
	protected String[] getJNLPInfo() {
		if (fJNLPTab == null || fTabFolder.getItemCount() < 4)
			return null;
		return fJNLPTab.getJNLPInfo();
	}
	
	public IWizardPage getNextPage() {
		return doMultiPlatform() ? getWizard().getNextPage(this) : null;
	}
	
}
