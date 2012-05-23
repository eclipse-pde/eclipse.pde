/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;

public class OrganizeManifestsWizard extends RefactoringWizard {

	private OrganizeManifestsWizardPage fMainPage;

	public OrganizeManifestsWizard(PDERefactor refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.OrganizeManifestsWizard_title);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_ORGANIZE_MANIFESTS);
	}

	public boolean performFinish() {
		fMainPage.performOk();
		return super.performFinish();
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());

		// Collect any custom build projects and warn the user
		Set customProjects = getProjectsWithCustomBuild(((PDERefactor) getRefactoring()).getProcessor().getElements());
		fMainPage = new OrganizeManifestsWizardPage(customProjects);
		addPage(fMainPage);
	}

	/**
	 * Collects a list of projects that have a build.properties with the custom=true property
	 * as the organize manifest processor may not handle it well.
	 * 
	 * @param elements the elements the refactoring is applying to, usually IProjects
	 * @return a list of IProjects that have the custom build property set, possibly empty
	 */
	private Set/*<IProject>*/getProjectsWithCustomBuild(Object[] elements) {
		Set result = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			try {
				if (elements[i] instanceof IResource) {
					IProject project = ((IResource) elements[i]).getProject();
					if (project != null) {
						if (project.hasNature(IBundleProjectDescription.PLUGIN_NATURE)) {
							IPluginModelBase pluginModel = PDECore.getDefault().getModelManager().findModel(project);
							if (pluginModel != null) {
								IBuildModel buildModel = PluginRegistry.createBuildModel(pluginModel);
								if (buildModel != null) {
									if (getCustomSelection(buildModel)) {
										result.add(project);
									}
								}
							}
						}
					}
				}
			} catch (CoreException e) {
				// Ignore bundles with problems
			}
		}
		return result;
	}

	private boolean getCustomSelection(IBuildModel model) {
		IBuild build = model.getBuild();
		IBuildEntry customEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_CUSTOM);
		if (customEntry == null || customEntry.getTokens().length == 0)
			return false;
		return customEntry.getTokens()[0].equals("true"); //$NON-NLS-1$
	}
}
