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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeDeleteParticipant extends PDEDeleteParticipant {

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IType type) {
			IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElements = new HashMap<>();
				fElements.put(element, type.getFullyQualifiedName());
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestTypeDeleteParticipant_composite;
	}

	@Override
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (!file.exists()) {
			return;
		}

		Map<IPackageFragment, List<IType>> deletedByPackage = new HashMap<>();
		fElements.forEach((element, fullyQualifiedName) -> {
			if (element instanceof IType type) {
				IPackageFragment pkg = type.getPackageFragment();
				deletedByPackage.computeIfAbsent(pkg, k -> new ArrayList<>()).add(type);
			}
		});
		// Check each package to see if it becomes empty after deletion
		for (Map.Entry<IPackageFragment, List<IType>> entry : deletedByPackage.entrySet()) {
			IPackageFragment pkg = entry.getKey();
			List<IType> deletedTypes = entry.getValue();

			try {
				if (willPackageBeEmpty(pkg, deletedTypes)) {
					Change change = BundleManifestChange.createEmptyPackageChange(file, pkg.getElementName(), pm);
					if (change != null) {
						result.add(change);
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks if a package will be empty after deleting the specified types.
	 * @param pkg the package to check
	 * @param deletedTypes the types being deleted
	 * @return true if the package will be empty after deletion
	 * @throws CoreException if an error occurs accessing package contents
	 */
	private boolean willPackageBeEmpty(IPackageFragment pkg, List<IType> deletedTypes) throws CoreException {
		IJavaElement[] javaChildren = pkg.getChildren();
		if (javaChildren.length > deletedTypes.size()) {
			return false;
		}
		// Check for non-Java resources (properties files, XML files, etc.)
		Object[] nonJavaResources = pkg.getNonJavaResources();
		if (nonJavaResources != null && nonJavaResources.length > 0) {
			return false;
		}

		Set<String> deletedJavaFileNames = new HashSet<>();
		for (IType type : deletedTypes) {
			// Get the compilation unit (the .java file) containing this type
			if (type.getCompilationUnit() != null) {
				deletedJavaFileNames.add(type.getCompilationUnit().getElementName());
			}
		}
		// Check the underlying folder for any OTHER files
		IResource resource = pkg.getCorrespondingResource();
		if (resource instanceof IFolder folder) {
			IResource[] members = folder.members();
			for (IResource member : members) {
				if (member instanceof IFile memberFile) {
					String fileName = memberFile.getName();
					String extension = memberFile.getFileExtension();
					if ("class".equals(extension)) { //$NON-NLS-1$
						continue;
					}
					if ("java".equals(extension) && deletedJavaFileNames.contains(fileName)) { //$NON-NLS-1$
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}

}
