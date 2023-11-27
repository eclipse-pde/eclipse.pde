/*******************************************************************************
 * Copyright (c) 2014, 2017 Red Hat Inc., and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class FeatureProjectConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		IFile featureFile = project.getFile(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
		if (featureFile.exists()) {
			WorkspaceFeatureModel workspaceFeatureModel = new WorkspaceFeatureModel(featureFile);
			workspaceFeatureModel.load();
			return workspaceFeatureModel.isLoaded();
		}
		return featureFile.exists();
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		if (!FeatureProject.isFeatureProject(project)) {
			try {
				CoreUtility.addNatureToProject(project, FeatureProject.NATURE, monitor);
			} catch (CoreException ex) {
				PDEPlugin.log(ex);
			}
		}
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(IPath.fromOSString(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)).exists();
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// Not really easy to spot PDE projects from a given directory
		// Moreover PDE projects are often expected to have a .project, which is supported
		// by EclipseProjectConfigurator
		return null;
	}

}
