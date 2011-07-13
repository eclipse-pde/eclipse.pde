/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public abstract class AbstractNewFeatureWizard extends NewWizard implements IExecutableExtension {

	public static final String DEF_PROJECT_NAME = "project-name"; //$NON-NLS-1$
	public static final String DEF_FEATURE_ID = "feature-id"; //$NON-NLS-1$
	public static final String DEF_FEATURE_NAME = "feature-name"; //$NON-NLS-1$

	protected AbstractFeatureSpecPage fSpecPage;
	protected PluginListPage fSecondPage;
	protected FeaturePatchProvider fProvider;
	private IConfigurationElement fConfig;

	public class FeaturePatchProvider implements IProjectProvider {
		public FeaturePatchProvider() {
			super();
		}

		public String getProjectName() {
			return fSpecPage.getProjectName();
		}

		public IProject getProject() {
			return fSpecPage.getProjectHandle();
		}

		public IPath getLocationPath() {
			return fSpecPage.getLocationPath();
		}

		public IFeatureModel getFeatureToPatch() {
			return fSpecPage.getFeatureToPatch();
		}

		public FeatureData getFeatureData() {
			return fSpecPage.getFeatureData();
		}

		public String getInstallHandlerLibrary() {
			return fSpecPage.getInstallHandlerLibrary();
		}

		public IPluginBase[] getPluginListSelection() {
			return fSecondPage != null ? fSecondPage.getSelectedPlugins() : null;
		}

		public ILaunchConfiguration getLaunchConfiguration() {
			return fSecondPage != null ? fSecondPage.getSelectedLaunchConfiguration() : null;
		}
	}

	public AbstractNewFeatureWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		fSpecPage = createFirstPage();
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null)
			fSpecPage.setInitialProjectName(pname);

		fSpecPage.setInitialId(getDefaultValue(DEF_FEATURE_ID));
		fSpecPage.setInitialName(getDefaultValue(DEF_FEATURE_NAME));
		addPage(fSpecPage);

		fProvider = new FeaturePatchProvider();
	}

	protected abstract AbstractFeatureSpecPage createFirstPage();

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return ((page == fSpecPage && page.isPageComplete()) || (page == fSecondPage && page.isPageComplete()));
	}

	// get creation operation
	protected abstract IRunnableWithProgress getOperation();

	public boolean performFinish() {
		try {
			IDialogSettings settings = getDialogSettings();
			fSpecPage.saveSettings(settings);
			if (settings != null && fSecondPage != null)
				fSecondPage.saveSettings(settings);

			getContainer().run(false, true, getOperation());
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public void setInitializationData(IConfigurationElement config, String property, Object data) throws CoreException {
		this.fConfig = config;
	}

}
