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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Handles class deletion in PDE plugin projects. Updates the bundle's MANIFEST.MF
 * when a package becomes empty after deleting types.
 */
public class ManifestTypeDeleteParticipant extends PDEDeleteParticipant {

	private IProject fProject;
	private Set<IType> fTypes = new LinkedHashSet<>();

	@Override
	protected boolean initialize(Object element) {
		IType type = (IType) element;
		IJavaProject javaProject = type.getJavaProject();
		if (javaProject != null) {
			fProject = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(fProject)) {
				fTypes.add(type);
				return true;
			}
		}
		return false;
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		IType type = (IType) element;
		fTypes.add(type);
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestTypeDeleteParticipant_composite;
	}

	@Override
	protected void addChanges(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (!file.exists()) {
			return;
		}
		// Group deleted types by package and collect their compilation units
		Map<IPackageFragment, Set<ICompilationUnit>> deletedCUsByPackage = new HashMap<>();
		for (IType type : fTypes) {
			IPackageFragment pkg = type.getPackageFragment();
			ICompilationUnit cu = type.getCompilationUnit();
			if (cu != null) {
				deletedCUsByPackage.computeIfAbsent(pkg, k -> new HashSet<>()).add(cu);
			}
		}
		// Check each package to see if it becomes empty after deletion
		List<IPackageFragment> emptiedPackages = deletedCUsByPackage.entrySet().stream().filter(e -> {
			IPackageFragment pkg = e.getKey();
			Set<ICompilationUnit> deletedCUs = e.getValue();
			try {
				return willPackageBeEmpty(pkg, deletedCUs);
			} catch (CoreException ex) {
				return false;
			}
		}).map(Map.Entry::getKey).toList();

		Change change = BundleManifestChange.createDeletePackagesChange(file, emptiedPackages, pm);
		if (change != null) {
			result.add(change);
		}

	}

	/**
	 * Checks if a package will be empty after deleting the specified compilation units.
	 *
	 * @param pkg the package to check
	 * @param deletedCUs the compilation units being deleted
	 * @return true if the package will be empty after deletion
	 * @throws CoreException if an error occurs accessing package contents
	 */
	private boolean willPackageBeEmpty(IPackageFragment pkg, Set<ICompilationUnit> deletedCUs) throws CoreException {
		// Check for non-Java resources (properties files, XML files, etc.)
		Object[] nonJavaResources = pkg.getNonJavaResources();
		if (nonJavaResources != null && nonJavaResources.length > 0) {
			return false;
		}
		// Check if any compilation unit in the package is NOT being deleted
		ICompilationUnit[] compilationUnits = pkg.getCompilationUnits();
		for (ICompilationUnit cu : compilationUnits) {
			if (!deletedCUs.contains(cu)) {
				// This compilation unit is not being deleted, so package is not
				// empty
				return false;
			}
		}
		return true;
	}

}
