/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Team Azure - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * An InternationalizeWizard is responsible for internationalizing a list of
 * specified plug-ins (workspace and external) to a set of specified locales.
 * This involves creating an NLS fragment project for every plug-in, which would
 * contain properties files for each specified locale. The first page of the wizard
 * allows the user to select the desired plug-ins and the second page the desired
 * locales. Also, the wizard ensures the plug-ins are externalized before proceeding
 * with internationlization.
 *
 */
public class InternationalizeWizard extends Wizard implements IImportWizard {
	private static final String STORE_SECTION = "InternationalizeWizard"; //$NON-NLS-1$

	private InternationalizeWizardPluginPage page1;
	private InternationalizeWizardLocalePage page2;

	//Contains the list of plug-ins to be internationalized
	private InternationalizeModelTable fInternationalizePluginModelTable;

	//Contains the list of locales
	private InternationalizeModelTable fInternationalizeLocaleModelTable;

	public InternationalizeWizard(InternationalizeModelTable pluginTable) {
		fInternationalizePluginModelTable = pluginTable;
		populateLocaleModelTable();
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_XHTML_CONVERT_WIZ);
		setWindowTitle(PDEUIMessages.InternationalizeWizard_title);
	}

	/**
	 * Populates the local InternationalizeModelTable with the list of all
	 * available locales
	 */
	private void populateLocaleModelTable() {
		fInternationalizeLocaleModelTable = new InternationalizeModelTable();
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (Locale locale : availableLocales) {
			fInternationalizeLocaleModelTable.addModel(locale);
		}
	}

	/**
	 * Initialises selections
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	/**
	 * Adds the plug-in and locale pages to the wizard
	 */
	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);
		page1 = new InternationalizeWizardPluginPage(fInternationalizePluginModelTable, "Plug-ins"); //$NON-NLS-1$
		addPage(page1);

		page2 = new InternationalizeWizardLocalePage(fInternationalizeLocaleModelTable, "Locales"); //$NON-NLS-1$
		addPage(page2);
	}

	/**
	 *
	 * @param master
	 * @return the created setting for the InternationalizeWizard
	 */
	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	/**
	 *
	 * @return the list of plug-ins selected for internationalization
	 */
	private List<?> getPluginModelsForInternationalization() {
		return page1.getModelsToInternationalize();
	}

	/**
	 *
	 * @return the list of locales specified for internationalization
	 */
	private List<?> getLocalesForInternationalization() {
		return page2.getLocalesForInternationalization();
	}

	@Override
	public boolean performFinish() {
		page1.storeSettings();
		page2.storeSettings();

		//Generate an NL fragment project for each of the selected plug-ins with the specified locales
		String template = page1.getTemplate() + (page1.createIndividualFragments() ? "." + NLSFragmentGenerator.LOCALE_NAME_MACRO : ""); //$NON-NLS-1$ //$NON-NLS-2$
		NLSFragmentGenerator fragmentGenerator = new NLSFragmentGenerator(template, getPluginModelsForInternationalization(), getLocalesForInternationalization(), this.getContainer(), page1.overwriteWithoutAsking());
		return fragmentGenerator.generate();
	}

	/**
	 *
	 * @param currentPage
	 * @return the next wizard page
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
		if (currentPage.equals(page1)) {
			ensurePluginsAreExternalized();
			return page2;
		}
		return null;
	}

	/**
	 *
	 * @param currentPage
	 * @return the previous wizard page
	 */
	@Override
	public IWizardPage getPreviousPage(IWizardPage currentPage) {
		if (currentPage.equals(page2)) {
			return page1;
		}
		return null;
	}

	@Override
	public boolean canFinish() {
		return !getPluginModelsForInternationalization().isEmpty() && !getLocalesForInternationalization().isEmpty();
	}

	/**
	 * Checks whether or not the selected plug-ins are already externalized. This
	 * method invokes the ExternalizeStringsWizard on the selected plug-ins.
	 */
	public void ensurePluginsAreExternalized() {
		GetNonExternalizedStringsAction externalize = new GetNonExternalizedStringsAction();

		List<IProject> projects = new ArrayList<>();
		List<?> pluginModels = getPluginModelsForInternationalization();

		for (Object pluginModelObject : pluginModels) {
			IPluginModelBase pluginModel = (IPluginModelBase) pluginModelObject;
			//Externalize only workspace plug-ins since external plug-ins are already externalized
			if (!(pluginModel instanceof ExternalPluginModel)) {
				IProject project = pluginModel.getUnderlyingResource().getProject();
				projects.add(project);
			}
		}

		//Set the selection for the non-externalized plug-ins that
		//should be passed to the ExternalizeStringsWizard
		IStructuredSelection externalizeSelection = new StructuredSelection(projects);
		externalize.setExternalizeSelectedPluginsOnly(true);
		externalize.setSkipMessageDialog(true);
		externalize.runGetNonExternalizedStringsAction(externalizeSelection);
	}

	@Override
	public boolean performCancel() {
		return super.performCancel();
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		super.setContainer(wizardContainer);
		if (getContainer() instanceof TrayDialog)
			((TrayDialog) getContainer()).setHelpAvailable(false);
	}

}
