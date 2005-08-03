/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewLibraryPluginProjectWizard extends NewWizard implements
		IExecutableExtension {
	public static final String DEF_PROJECT_NAME = "project_name"; //$NON-NLS-1$

	public static final String DEF_TEMPLATE_ID = "template-id"; //$NON-NLS-1$

	public static final String PLUGIN_POINT = "pluginContent"; //$NON-NLS-1$

	public static final String TAG_WIZARD = "wizard"; //$NON-NLS-1$

	private IConfigurationElement fConfig;

	private LibraryPluginJarsPage fJarsPage;

	private NewLibraryPluginCreationPage fMainPage;

	private LibraryPluginFieldData fPluginData;

	private IProjectProvider fProjectProvider;

	public NewLibraryPluginProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_JAR_TO_PLUGIN_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.NewLibraryPluginProjectWizard_title); 
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fPluginData = new LibraryPluginFieldData();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fJarsPage = new LibraryPluginJarsPage("jars", fPluginData); //$NON-NLS-1$ 
		addPage(fJarsPage);
		fMainPage = new NewLibraryPluginCreationPage("main", fPluginData); //$NON-NLS-1$
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null)
			fMainPage.setInitialProjectName(pname);

		fProjectProvider = new IProjectProvider() {
			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}

			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}

			public String getProjectName() {
				return fMainPage.getProjectName();
			}
		};

		addPage(fMainPage);
	}

	protected WizardElement createWizardElement(IConfigurationElement config) {
		String name = config.getAttribute(WizardElement.ATT_NAME);
		String id = config.getAttribute(WizardElement.ATT_ID);
		String className = config.getAttribute(WizardElement.ATT_CLASS);
		if (name == null || id == null || className == null)
			return null;
		WizardElement element = new WizardElement(config);
		String imageName = config.getAttribute(WizardElement.ATT_ICON);
		if (imageName != null) {
			String pluginID = config.getNamespace();
			Image image = PDEPlugin.getDefault().getLabelProvider()
					.getImageFromPlugin(pluginID, imageName);
			element.setImage(image);
		}
		return element;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			fJarsPage.updateData();
			fMainPage.updateData();
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			getContainer().run(
					false,
					true,
					new NewLibraryPluginCreationOperation(fPluginData,
							fProjectProvider, null));
			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
	 *      java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

}
