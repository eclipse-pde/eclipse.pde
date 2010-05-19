/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.importing.IBundleImporter;
import org.eclipse.pde.internal.core.importing.provisional.BundleImportDescription;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.provisional.IBundeImportWizardPage;

/**
 * Wizard to import plug-ins from a repository.
 * 
 * @since 3.6
 */
public class RepositoryImportWizard extends Wizard {

	/**
	 * Map of import delegates to import descriptions as provided by the {@link BundleProjectService}
	 */
	private Map fImportMap;

	/**
	 * Map of importer identifier to associated wizard import page
	 */
	private Map fIdToPages = new HashMap();

	private static final String STORE_SECTION = "RepositoryImportWizard"; //$NON-NLS-1$

	/**
	 * Map of import delegates to import descriptions.
	 * 
	 * @param importMap
	 */
	public RepositoryImportWizard(Map importMap) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.ImportWizard_title);
		fImportMap = importMap;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		Iterator iterator = fImportMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			IBundleImporter importer = (IBundleImporter) entry.getKey();
			BundleImportDescription[] descriptions = (BundleImportDescription[]) entry.getValue();
			IBundeImportWizardPage page = (IBundeImportWizardPage) fIdToPages.get(importer.getId());
			if (page == null) {
				page = getImportPage(importer.getId());
				if (page != null) {
					fIdToPages.put(importer.getId(), page);
					addPage(page);
					page.setSelection(descriptions);
				}
			}
		}
	}

	/**
	 * Creates and returns a wizard page associated with the given bundle importer extension identifier
	 * or <code>null</code> of none.
	 * 
	 * @param importerId org.eclipse.pde.core.bundleImporters extension identifier
	 * @return associated bundle import wizard page or <code>null</code>
	 */
	private IBundeImportWizardPage getImportPage(String importerId) {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(IPDEUIConstants.EXTENSION_POINT_BUNDLE_IMPORT_PAGES);
		if (point != null) {
			IConfigurationElement[] infos = point.getConfigurationElements();
			for (int i = 0; i < infos.length; i++) {
				IConfigurationElement element = infos[i];
				String id = element.getAttribute("bundleImporter"); //$NON-NLS-1$
				if (id != null && importerId.equals(id)) {
					try {
						return (IBundeImportWizardPage) element.createExecutableExtension("class"); //$NON-NLS-1$
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
			}
		}
		return null;
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// collect the bundle descriptions from each page and import
		Map importMap = new HashMap();
		List plugins = new ArrayList();
		IWizardPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			IBundeImportWizardPage page = (IBundeImportWizardPage) pages[i];
			if (page.finish()) {
				BundleImportDescription[] descriptions = page.getSelection();
				if (descriptions != null && descriptions.length > 0) {
					for (int j = 0; j < descriptions.length; j++) {
						BundleImportDescription description = descriptions[j];
						if (j == 0) {
							Object importer = description.getProperty(BundleProjectService.BUNDLE_IMPORTER);
							if (importer != null) {
								importMap.put(importer, descriptions);
							}
						}
						Object plugin = description.getProperty(BundleProjectService.PLUGIN);
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
			PluginImportWizard.doImportOperation(PluginImportOperation.IMPORT_FROM_REPOSITORY, (IPluginModelBase[]) plugins.toArray(new IPluginModelBase[plugins.size()]), false, false, null, importMap);
		}
		return true;
	}
}
