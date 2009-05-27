/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.BundleLauncherHelper;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard"; //$NON-NLS-1$

	private IStructuredSelection selection;
	private PluginImportWizardFirstPage page1;
	private BaseImportWizardSecondPage page2;
	private BaseImportWizardSecondPage page3;

	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.ImportWizard_title);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public void addPages() {
		setNeedsProgressMonitor(true);
		page1 = new PluginImportWizardFirstPage("first"); //$NON-NLS-1$
		addPage(page1);
		page2 = new PluginImportWizardExpressPage("express", page1, selection); //$NON-NLS-1$
		addPage(page2);
		page3 = new PluginImportWizardDetailedPage("detailed", page1); //$NON-NLS-1$
		addPage(page3);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	private IPluginModelBase[] getModelsToImport() {
		if (page1.getNextPage().equals(page2))
			return page2.getModelsToImport();
		return page3.getModelsToImport();
	}

	public boolean performFinish() {
		page1.storeSettings();
		((BaseImportWizardSecondPage) page1.getNextPage()).storeSettings();
		final IPluginModelBase[] models = getModelsToImport();
		int launchedConfiguration = getConflictingConfigurationsCount(models);
		if (launchedConfiguration > 0) {
			String message = launchedConfiguration == 1 ? PDEUIMessages.PluginImportWizard_runningConfigDesc : PDEUIMessages.PluginImportWizard_runningConfigsDesc;
			MessageDialog dialog = new MessageDialog(getShell(), PDEUIMessages.PluginImportWizard_runningConfigsTitle, null, message, MessageDialog.WARNING, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;

		}
		doImportOperation(getShell(), page1.getImportType(), models, page2.forceAutoBuild(), launchedConfiguration > 0, page1.getAlternateSourceLocations());
		return true;
	}

	/**
	 * @return the number of conflicting running launch configurations
	 */
	private int getConflictingConfigurationsCount(IPluginModelBase[] modelsToImport) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		int count = 0;
		ILaunch[] launches = launchManager.getLaunches();
		HashSet imported = new HashSet((4 * modelsToImport.length) / 3 + 1);
		for (int j = 0; j < modelsToImport.length; ++j) {
			BundleDescription bd = modelsToImport[j].getBundleDescription();
			if (bd != null) {
				imported.add(bd.getSymbolicName());
			}
		}
		for (int i = 0; i < launches.length; ++i) {
			if (!launches[i].isTerminated()) {
				ILaunchConfiguration configuration = launches[i].getLaunchConfiguration();
				if (configuration == null)
					continue;
				try {
					Map workspaceBundleMap = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
					for (Iterator iter = workspaceBundleMap.keySet().iterator(); iter.hasNext();) {
						IPluginModelBase bm = (IPluginModelBase) iter.next();
						BundleDescription description = bm.getBundleDescription();
						if (description != null) {
							if (imported.contains(description.getSymbolicName())) {
								++count;
								break;
							}
						}

					}
				} catch (CoreException e) {
					++count;
				}
			}
		}
		return count;
	}

	public static void doImportOperation(Shell shell, int importType, IPluginModelBase[] models, boolean forceAutobuild) {
		doImportOperation(shell, importType, models, forceAutobuild, false, null);
	}

	/**
	 * 
	 * @param shell
	 * @param importType
	 * @param models
	 * @param forceAutobuild
	 * @param launchedConfiguration
	 * @param alternateSource used to locate source attachments or <code>null</code> if default
	 * 	source locations should be used (from active target platform).
	 *  
	 */
	private static void doImportOperation(Shell shell, int importType, IPluginModelBase[] models, boolean forceAutobuild, boolean launchedConfiguration, SourceLocationManager alternateSource) {
		PluginImportOperation job = new PluginImportOperation(models, importType, forceAutobuild);
		job.setAlternateSource(alternateSource);
		job.setPluginsInUse(launchedConfiguration);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(page1)) {
			if (page1.getScanAllPlugins()) {
				return page3;
			}
			return page2;
		}
		return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return page.equals(page1) ? null : page1;
	}

	public boolean canFinish() {
		return !page1.isCurrentPage() && page1.getNextPage().isPageComplete();
	}
}
