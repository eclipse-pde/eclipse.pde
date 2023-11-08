/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

@SuppressWarnings("restriction")
//The IBundleImporter API is currently provisional
public class PluginImportWizard extends Wizard implements IImportWizard, IPageChangingListener {

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

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		IWizardContainer container = getContainer();
		if (container instanceof WizardDialog dialog) {
			dialog.addPageChangingListener(this);
		}
	}

	@Override
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

	@Override
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
		if (page1.getImportType() == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			if (getContainer().getCurrentPage() instanceof BaseImportWizardSecondPage) {
				// ensure to set the models to import when finished is pressed without advancing to the repository pages
				page1.configureBundleImportPages(models);
			}
		}
		// finish contributed pages
		if (!page1.finishPages()) {
			return false;
		}
		doImportOperation(page1.getImportType(), models, page2.forceAutoBuild(), launchedConfiguration > 0, page1.getAlternateSourceLocations(), page1.getImportDescriptions());

		return true;
	}

	/**
	 * @return the number of conflicting running launch configurations
	 */
	private int getConflictingConfigurationsCount(IPluginModelBase[] modelsToImport) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		int count = 0;
		ILaunch[] launches = launchManager.getLaunches();
		HashSet<String> imported = new HashSet<>((4 * modelsToImport.length) / 3 + 1);
		for (IPluginModelBase model : modelsToImport) {
			BundleDescription bd = model.getBundleDescription();
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
					var workspaceBundles = BundleLauncherHelper.getWorkspaceBundleMap(configuration).keySet();
					for (IPluginModelBase bm : workspaceBundles) {
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
		doImportOperation(importType, models, forceAutobuild, false, null, null);
	}

	/**
	 * Performs the import.
	 *
	 * @param importType one of the import operation types
	 * @param models models being imported
	 * @param forceAutobuild whether to force a build after the import
	 * @param launchedConfiguration if there is a launched target currently running
	 * @param alternateSource used to locate source attachments or <code>null</code> if default
	 * 	source locations should be used (from active target platform).
	 * @param importerToDescriptions map of bundle importers to import descriptions if importing
	 *  from a repository, else <code>null</code>
	 *
	 */
	public static void doImportOperation(int importType, IPluginModelBase[] models, boolean forceAutobuild, boolean launchedConfiguration, SourceLocationManager alternateSource, Map<IBundleImporter, ScmUrlImportDescription[]> importerToDescriptions) {
		PluginImportOperation job = new PluginImportOperation(models, importType, forceAutobuild);
		job.setImportDescriptions(importerToDescriptions);
		job.setAlternateSource(alternateSource);
		job.setPluginsInUse(launchedConfiguration);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(page1)) {
			if (page1.getScanAllPlugins()) {
				return page3;
			}
			return page2;
		}
		if (page.equals(page2) || page.equals(page3) || (page instanceof IScmUrlImportWizardPage)) {
			IPluginModelBase[] models = getModelsToImport();
			page1.configureBundleImportPages(models);
			return page1.getNextPage(page);
		}

		return null;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page.equals(page1)) {
			return null;
		}
		if (page.equals(page2) || page.equals(page3)) {
			return page1;
		}
		IWizardPage prev = page1.getPreviousPage(page);
		if (prev == null) {
			return page3;
		}
		return prev;
	}

	@Override
	public boolean canFinish() {
		return !page1.isCurrentPage() && page1.getNextPage().isPageComplete() && page1.arePagesComplete();
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() instanceof BaseImportWizardSecondPage && event.getTargetPage() instanceof IScmUrlImportWizardPage) {
			IPluginModelBase[] models = getModelsToImport();
			page1.configureBundleImportPages(models);
		}
	}
}
