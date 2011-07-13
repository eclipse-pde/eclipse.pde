/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewFragmentProjectWizard extends NewWizard implements IExecutableExtension {

	private NewProjectCreationPage fMainPage;
	private ContentPage fContentPage;
	private FragmentFieldData fFragmentData;
	private IProjectProvider fProjectProvider;
	private IConfigurationElement fConfig;

	public NewFragmentProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.NewFragmentProjectWizard_title);
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fFragmentData = new FragmentFieldData();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new NewProjectCreationPage("main", fFragmentData, true, getSelection()); //$NON-NLS-1$
		fMainPage.setTitle(PDEUIMessages.NewProjectWizard_MainPage_ftitle);
		fMainPage.setDescription(PDEUIMessages.NewProjectWizard_MainPage_fdesc);
		addPage(fMainPage);

		fProjectProvider = new IProjectProvider() {
			public String getProjectName() {
				return fMainPage.getProjectName();
			}

			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}

			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}
		};
		fContentPage = new FragmentContentPage("page2", fProjectProvider, fMainPage, fFragmentData); //$NON-NLS-1$
		addPage(fContentPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return (page.isPageComplete() && page != fMainPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			fMainPage.updateData();
			fContentPage.updateData();
			IDialogSettings settings = getDialogSettings();
			if (settings != null) {
				fMainPage.saveSettings(settings);
				fContentPage.saveSettings(settings);
			}

			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			getContainer().run(false, true, new NewProjectCreationOperation(fFragmentData, fProjectProvider, null));

			IWorkingSet[] workingSets = fMainPage.getSelectedWorkingSets();
			getWorkbench().getWorkingSetManager().addToWorkingSets(fProjectProvider.getProject(), workingSets);

			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

	public String getFragmentId() {
		return fFragmentData.getId();
	}

	public String getFragmentVersion() {
		return fFragmentData.getVersion();
	}
}
