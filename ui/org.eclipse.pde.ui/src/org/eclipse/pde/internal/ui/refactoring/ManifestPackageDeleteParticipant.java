/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Handles package deletion in PDE plugin projects. Updates the bundle's
 * MANIFEST.MF after deleting packages.
 */
public class ManifestPackageDeleteParticipant extends PDEDeleteParticipant {

	private IProject fProject;
	private Set<IPackageFragment> fPackages = new HashSet<>();

	@Override
	protected boolean initialize(Object element) {
		IPackageFragment fragment = (IPackageFragment) element;
		try {
			if (!fragment.containsJavaResources()) {
				return false;
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
		fProject = fragment.getJavaProject().getProject();
		if (WorkspaceModelManager.isPluginProject(fProject)) {
			fPackages.add(fragment);
			return true;
		}
		return false;
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		fPackages.add((IPackageFragment) element);
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestPackageDeleteParticipant_packageDelete;
	}

	@Override
	protected void addChanges(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (file.exists()) {
			Change change = BundleManifestChange.createDeletePackagesChange(file, fPackages, pm);
			if (change != null) {
				result.add(change);
			}
		}
	}

}
