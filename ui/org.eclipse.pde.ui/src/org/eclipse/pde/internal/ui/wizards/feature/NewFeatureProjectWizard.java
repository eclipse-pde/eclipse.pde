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
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewFeatureProjectWizard
	extends NewWizard
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewFeatureWizard.MainPage.title";
	public static final String MAIN_PAGE_DESC = "NewFeatureWizard.MainPage.desc";
	public static final String DEF_PROJECT_NAME="project-name";
	public static final String DEF_FEATURE_ID="feature-id";
	public static final String DEF_FEATURE_NAME="feature-name";

	private WizardNewProjectCreationPage mainPage;
	private FeatureSpecPage specPage;
	private PluginListPage pluginListPage;
	private FeatureCustomHandlerPage structurePage;
	private IConfigurationElement config;

	public class FeatureProjectProvider implements IProjectProvider {
		public FeatureProjectProvider(){
			super();
		}
		public String getProjectName() {
			return mainPage.getProjectName();
		}
		public IProject getProject() {
			return mainPage.getProjectHandle();
		}
		public IPath getLocationPath() {
			return mainPage.getLocationPath();
		}
		public FeatureData getFeatureData(){
			return specPage.getFeatureData();
		}
		public IPluginBase[] getPluginListSelection(){
			if (pluginListPage == null)
				return null;
			return pluginListPage.getSelectedPlugins();
		}
		public IConfigurationElement getConfigElement(){
			return config;
		}
	}
	public NewFeatureProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	public void addPages() {
		mainPage = new WizardNewProjectCreationPage("main");
		mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname!=null)
			mainPage.setInitialProjectName(pname);
		addPage(mainPage);
		IProjectProvider provider = new FeatureProjectProvider();
		specPage = new FeatureSpecPage(mainPage);
		specPage.setInitialId(getDefaultValue(DEF_FEATURE_ID));
		specPage.setInitialName(getDefaultValue(DEF_FEATURE_NAME));
		addPage(specPage);
		structurePage = new FeatureCustomHandlerPage(provider);
		addPage(structurePage);
		if (hasInterestingProjects()) {
			pluginListPage = new PluginListPage();
			addPage(pluginListPage);
		}
	}
	
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return ((page == specPage && structurePage.isInitialized())
			|| (page == structurePage && page.isPageComplete()) 
			|| (page == pluginListPage && page.isPageComplete()));
	}

	private boolean hasInterestingProjects() {
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (WorkspaceModelManager.isPluginProject(project))
				return true;
		}
		return false;
	}

	public boolean performFinish() {
		return structurePage.finish();

	}

	public void setInitializationData(
		IConfigurationElement config,
		String property,
		Object data)
		throws CoreException {
		this.config = config;
	}
}
