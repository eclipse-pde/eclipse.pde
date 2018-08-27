/*******************************************************************************
 * Copyright (c) 2011, 2013, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.team.ui.TeamUI;

/**
 * Wizard to import plug-ins from a repository.
 *
 * @since 3.6
 */
@SuppressWarnings("restriction")
//The IBundleImporter API is currently provisional
public class RepositoryImportWizard extends Wizard {

	/**
	 * Map of import delegates to import descriptions as provided by the {@link BundleProjectService}
	 */
	private Map<?, ?> fImportMap;

	/**
	 * Map of importer identifier to associated wizard import page
	 */
	private Map<String, IScmUrlImportWizardPage> fIdToPages = new HashMap<>();

	private static final String STORE_SECTION = "RepositoryImportWizard"; //$NON-NLS-1$

	/**
	 * Map of import delegates to import descriptions.
	 *
	 * @param importMap
	 */
	public RepositoryImportWizard(Map<?, ?> importMap) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.ImportWizard_title);
		fImportMap = importMap;
	}

	@Override
	public void addPages() {
		Iterator<?> iterator = fImportMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
			final IBundleImporter importer = (IBundleImporter) entry.getKey();
			final String importerId = importer.getId();
			ScmUrlImportDescription[] descriptions = (ScmUrlImportDescription[]) entry.getValue();
			IScmUrlImportWizardPage page = fIdToPages.get(importerId);
			if (page == null) {
				try {
					page = TeamUI.getPages(importerId)[0];
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
				if (page != null) {
					page.setSelection(descriptions);
					fIdToPages.put(importerId, page);
					addPage(page);
				}
			}
		}
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	@Override
	public boolean performFinish() {
		// collect the bundle descriptions from each page and import
		List<Object> plugins = new ArrayList<>();
		IWizardPage[] pages = getPages();
		Map<IBundleImporter, ScmUrlImportDescription[]> importMap = new HashMap<>();
		for (IWizardPage wizardPage : pages) {
			IScmUrlImportWizardPage page = (IScmUrlImportWizardPage) wizardPage;
			if (page.finish()) {
				ScmUrlImportDescription[] descriptions = page.getSelection();
				if (descriptions != null && descriptions.length > 0) {
					for (int j = 0; j < descriptions.length; j++) {
						if (j == 0) {
							Object importer = descriptions[j].getProperty(BundleProjectService.BUNDLE_IMPORTER);
							if (importer instanceof IBundleImporter) {
								importMap.put((IBundleImporter) importer, descriptions);
							}
						}
						Object plugin = descriptions[j].getProperty(BundleProjectService.PLUGIN);
						if (plugin != null) {
							plugins.add(plugin);
						}
					}
				}
			} else {
				return false;
			}
		}
		if (!importMap.isEmpty()) {
			PluginImportWizard.doImportOperation(PluginImportOperation.IMPORT_FROM_REPOSITORY, plugins.toArray(new IPluginModelBase[plugins.size()]), false, false, null, importMap);
		}
		return true;
	}
}
