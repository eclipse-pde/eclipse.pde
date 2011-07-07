/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.plugin.*;
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

	private AbstractFieldData fPluginData;
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
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
			public String getProjectName() {
				return fProjectPage.getProjectName();
			}

			public IProject getProject() {
				return fProjectPage.getProjectHandle();
			}

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
			for (int i = 0; i < pages.length; i++) {
				addPage(pages[i]);
			}
		} catch (CoreException e) {
			MessageDialog.openError(getShell(), PDEUIMessages.NewPluginProjectFromTemplateWizard_1, NLS.bind(PDEUIMessages.NewPluginProjectFromTemplateWizard_0, getTemplateID()));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		if (super.canFinish() && !getContainer().getCurrentPage().equals(fProjectPage)) {
			if (fTemplateWizard == null || fTemplateWizard.canFinish()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
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
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(TAG_WIZARD)) {
					if (templateID.equals(elements[j].getAttribute(WizardElement.ATT_ID))) {
						return WizardElement.create(elements[j]);
					}
				}
			}
		}
		return null;
	}

}
