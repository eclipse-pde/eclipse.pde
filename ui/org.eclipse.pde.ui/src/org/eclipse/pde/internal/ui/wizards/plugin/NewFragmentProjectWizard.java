/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewFragmentProjectWizard extends NewWizard implements IExecutableExtension {

	private WizardNewProjectCreationPage fMainPage;
	private ProjectStructurePage fStructurePage;
	private ContentPage fContentPage;
	private FragmentFieldData fFragmentData;
	private IProjectProvider fProjectProvider;
	private IConfigurationElement fConfig;
	
	public NewFragmentProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString("NewFragmentProjectWizard.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fFragmentData = new FragmentFieldData();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new WizardNewProjectCreationPage("main"); //$NON-NLS-1$
		fMainPage.setTitle(PDEPlugin.getResourceString("NewProjectWizard.MainPage.ftitle")); //$NON-NLS-1$
		fMainPage.setDescription(PDEPlugin.getResourceString("NewProjectWizard.MainPage.fdesc")); //$NON-NLS-1$
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
		
		fStructurePage = new ProjectStructurePage("page1", fProjectProvider, fFragmentData, true); //$NON-NLS-1$
		fContentPage = new ContentPage("page2", fProjectProvider, fStructurePage, fFragmentData, true); //$NON-NLS-1$
		addPage(fStructurePage);
		addPage(fContentPage);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			fStructurePage.updateData();
			fContentPage.updateData();
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			getContainer().run(false, true,
					new NewProjectCreationOperation(fFragmentData, fProjectProvider, null));
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
}
