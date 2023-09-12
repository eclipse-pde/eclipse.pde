/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.internal.ui.wizards.plugin.AbstractFieldData;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationFromTemplatePage;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationPage;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginContentPage;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * API class to allow customization of the new plug-in project wizard.  Extending
 * this class and implementing the {@link NewPluginProjectFromTemplateWizard#getTemplateID()}
 * method will create a wizard with the same UI components as the standard new plug-in
 * project wizard.  However, instead of asking the user to select a template, the template
 * is predetermined.  The template selection page will be skipped going directly to the
 * wizard associated with the template.  In addition, if the template requires certain
 * settings (determined by the flags set on the extension), the associated UI components
 * will be disabled so that the user cannot change their value.
 *
 * Other aspects of this wizard including the window title can be edited by overriding
 * methods in the subclass.
 *
 * @since 3.5
 */
public abstract class NewPluginProjectFromTemplateWizard extends NewWizard implements IExecutableExtension {

	/**
	 * Key for storing the project name in the wizard default values
	 */
	public static final String DEF_PROJECT_NAME = "project_name"; //$NON-NLS-1$

	/**
	 * Key for storign the extension point name in the wizard default values
	 */
	public static final String PLUGIN_POINT = "pluginContent"; //$NON-NLS-1$

	/**
	 * The extension element that contains the wizard class
	 */
	public static final String TAG_WIZARD = "wizard"; //$NON-NLS-1$

	private final AbstractFieldData fPluginData;
	private NewProjectCreationPage fProjectPage;
	private PluginContentPage fContentPage;
	private IPluginContentWizard fTemplateWizard;
	private IProjectProvider fProjectProvider;
	private IConfigurationElement fConfig;

	/**
	 * Constructor to create a new wizard
	 */
	public NewPluginProjectFromTemplateWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.NewProjectWizard_title);
		setNeedsProgressMonitor(true);
		fPluginData = new PluginFieldData();
	}

	/**
	 * Returns the string id of the template extension to use as the template for the
	 * new plug-in project wizard.  Must not return <code>null</code>.
	 * @return string id of the template to use
	 */
	protected abstract String getTemplateID();

	@Override
	public void addPages() {
		WizardElement templateWizardElement = getTemplateWizard();
		if (templateWizardElement == null) {
			MessageDialog.openError(getShell(), PDEUIMessages.NewPluginProjectFromTemplateWizard_1, NLS.bind(PDEUIMessages.NewPluginProjectFromTemplateWizard_0, getTemplateID()));
			return;
		}

		fProjectPage = new NewProjectCreationFromTemplatePage("main", fPluginData, getSelection(), templateWizardElement); //$NON-NLS-1$
		fProjectPage.setTitle(PDEUIMessages.NewProjectWizard_MainPage_title);
		fProjectPage.setDescription(PDEUIMessages.NewProjectWizard_MainPage_desc);

		String projectName = getDefaultValue(DEF_PROJECT_NAME);
		if (projectName != null)
			fProjectPage.setInitialProjectName(projectName);
		addPage(fProjectPage);

		fProjectProvider = new IProjectProvider() {
			@Override
			public String getProjectName() {
				return fProjectPage.getProjectName();
			}

			@Override
			public IProject getProject() {
				return fProjectPage.getProjectHandle();
			}

			@Override
			public IPath getLocationPath() {
				return fProjectPage.getLocationPath();
			}
		};

		fContentPage = new PluginContentPage("page2", fProjectProvider, fProjectPage, fPluginData); //$NON-NLS-1$
		addPage(fContentPage);

		try {
			fTemplateWizard = (IPluginContentWizard) templateWizardElement.createExecutableExtension();
			fTemplateWizard.init(fPluginData);
			fTemplateWizard.addPages();
			IWizardPage[] pages = fTemplateWizard.getPages();
			for (IWizardPage page : pages) {
				addPage(page);
			}
		} catch (CoreException e) {
			MessageDialog.openError(getShell(), PDEUIMessages.NewPluginProjectFromTemplateWizard_1, NLS.bind(PDEUIMessages.NewPluginProjectFromTemplateWizard_0, getTemplateID()));
		}
	}

	@Override
	public boolean canFinish() {
		if (super.canFinish() && !getContainer().getCurrentPage().equals(fProjectPage)) {
			if (fTemplateWizard == null || fTemplateWizard.canFinish()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean performFinish() {
		try {
			fProjectPage.updateData();
			fContentPage.updateData();
			IDialogSettings settings = getDialogSettings();
			if (settings != null) {
				fProjectPage.saveSettings(settings);
				fContentPage.saveSettings(settings);
			}
			BasicNewProjectResourceWizard.updatePerspective(fConfig);

			// If the PDE models are not initialized, initialize with option to cancel
			if (!PDECore.getDefault().areModelsInitialized()) {
				try {
					getContainer().run(true, true, monitor -> {
						// Target reloaded method clears existing models (which don't exist currently) and inits them with a progress monitor
						PDECore.getDefault().getModelManager().targetReloaded(monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					});
				} catch (InterruptedException e) {
					// Model initialization cancelled
					return false;
				}
			}

			getContainer().run(false, true, new NewProjectCreationOperation(fPluginData, fProjectProvider, fTemplateWizard));

			IWorkingSet[] workingSets = fProjectPage.getSelectedWorkingSets();
			if (workingSets.length > 0)
				getWorkbench().getWorkingSetManager().addToWorkingSets(fProjectProvider.getProject(), workingSets);

			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

	/**
	 * Creates a WizardElement representing the template extension to be used by this wizard.
	 *
	 * @return element representing the template or <code>null</code> if the extension could not be loaded
	 */
	private WizardElement getTemplateWizard() {
		String templateID = getTemplateID();
		if (templateID == null) {
			return null;
		}

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), PLUGIN_POINT);
		if (point == null) {
			return null;
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(TAG_WIZARD)) {
					if (templateID.equals(element.getAttribute(WizardElement.ATT_ID))) {
						return WizardElement.create(element);
					}
				}
			}
		}
		return null;
	}

}
