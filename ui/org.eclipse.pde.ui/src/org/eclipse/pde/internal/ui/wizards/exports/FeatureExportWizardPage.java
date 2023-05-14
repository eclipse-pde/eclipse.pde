/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
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
		super(selection, "featureExport", //$NON-NLS-1$
				PDEUIMessages.ExportWizard_Feature_pageBlock);
		setTitle(PDEUIMessages.ExportWizard_Feature_pageTitle);
	}

	@Override
	protected Object getInput() {
		return PDECore.getDefault().getFeatureModelManager();
	}

	@Override
	public Object[] getListElements() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		return manager.getWorkspaceModels();
	}

	@Override
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}

	@Override
	protected boolean isValidModel(IModel model) {
		return model instanceof IFeatureModel;
	}

	@Override
	protected void createTabs(TabFolder folder, IDialogSettings settings) {
		super.createTabs(folder, settings);
		if (fDestinationTab.doExportToDirectory() && fOptionsTab.useJARFormat()) {
			createJNLPTab(folder);
			fJNLPTab.initialize(settings);
		}
	}

	@Override
	protected void createDestinationTab(TabFolder folder) {
		fDestinationTab = new FeatureDestinationTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fDestinationTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_destination);
	}

	@Override
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

	@Override
	protected IModel findModelFor(IAdaptable object) {
		IProject project = object.getAdapter(IProject.class);
		if (project != null) {
			return PDECore.getDefault().getFeatureModelManager().getFeatureModel(project);
		}
		return null;
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fJNLPTab != null)
			fJNLPTab.saveSettings(settings);
	}

	@Override
	protected String validateTabs() {
		String message = super.validateTabs();
		if (message == null && fTabFolder.getItemCount() > 3)
			message = fJNLPTab.validate();
		return message;
	}

	@Override
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
			if (count >= (useJARFormat() ? 4 : 3)) {
				fJNLPTab.saveSettings(settings);
				fTabFolder.getItem(count - 1).dispose();
			}
		}
	}

	protected boolean doMultiPlatform() {
		return ((FeatureOptionsTab) fOptionsTab).doMultiplePlatform();
	}

	protected String[] getJNLPInfo() {
		if (fJNLPTab == null || fTabFolder.getItemCount() < 4)
			return null;
		return fJNLPTab.getJNLPInfo();
	}

	/**
	 * @return whether to generate p2 metadata on export
	 */
	protected boolean doExportMetadata() {
		if (fOptionsTab instanceof FeatureOptionsTab) {
			return ((FeatureOptionsTab) fOptionsTab).doExportMetadata();
		}
		return false;
	}

	/**
	 * @return the location of the category definition file.  Null if none specified.
	 */
	protected URI getCategoryDefinition() {
		if (fOptionsTab instanceof FeatureOptionsTab) {
			return ((FeatureOptionsTab) fOptionsTab).getCategoryDefinition();
		}
		return null;
	}

	@Override
	public IWizardPage getNextPage() {
		return doMultiPlatform() ? getWizard().getNextPage(this) : null;
	}

}
