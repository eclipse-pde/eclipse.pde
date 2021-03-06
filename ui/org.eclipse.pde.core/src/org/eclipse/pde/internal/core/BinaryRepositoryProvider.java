/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Fabio Mancinelli <fm@fabiomancinelli.org> - bug 201308
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;

public class BinaryRepositoryProvider extends RepositoryProvider {
	private final IMoveDeleteHook moveDeleteHook;
	private final FileModificationValidator fileModificationValidator;

	public static final String EXTERNAL_PROJECT_VALUE = "external"; //$NON-NLS-1$

	class BinaryMoveDeleteHook implements IMoveDeleteHook {
		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteFile(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFile, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags, IProgressMonitor monitor) {
			if (isBinaryResource(file, true)) {
				tree.failed(createProblemStatus());
			} else {
				tree.standardDeleteFile(file, updateFlags, monitor);
			}
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteFolder(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFolder, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean deleteFolder(IResourceTree tree, IFolder folder, int updateFlags, IProgressMonitor monitor) {
			if (isBinaryResource(folder, true)) {
				tree.failed(createProblemStatus());
			} else {
				tree.standardDeleteFolder(folder, updateFlags, monitor);
			}
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor) {
			return false;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveFile(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFile, org.eclipse.core.resources.IFile, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean moveFile(IResourceTree tree, IFile source, IFile destination, int updateFlags, IProgressMonitor monitor) {
			if (isBinaryResource(source, false)) {
				tree.failed(createProblemStatus());
			} else {
				tree.standardMoveFile(source, destination, updateFlags, monitor);
			}
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveFolder(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFolder, org.eclipse.core.resources.IFolder, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean moveFolder(IResourceTree tree, IFolder source, IFolder destination, int updateFlags, IProgressMonitor monitor) {
			if (isBinaryResource(source, false)) {
				tree.failed(createProblemStatus());
			} else {
				tree.standardMoveFolder(source, destination, updateFlags, monitor);
			}
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, org.eclipse.core.resources.IProjectDescription, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public boolean moveProject(IResourceTree tree, IProject source, IProjectDescription description, int updateFlags, IProgressMonitor monitor) {
			return false;
		}
	}

	class BinaryFileModificationValidator extends FileModificationValidator {
		@Override
		public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
			for (IFile file : files) {
				if (isBinaryResource(file, false)) {
					return createProblemStatus();
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		public IStatus validateSave(IFile file) {
			if (isBinaryResource(file, false)) {
				return createProblemStatus();
			}
			return Status.OK_STATUS;
		}
	}

	public BinaryRepositoryProvider() {
		moveDeleteHook = new BinaryMoveDeleteHook();
		fileModificationValidator = new BinaryFileModificationValidator();
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	@Override
	public void configureProject() throws CoreException {
		IProject project = getProject();
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, EXTERNAL_PROJECT_VALUE);
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
		IProject project = getProject();
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, null);
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getFileModificationValidator2()
	 */
	@Override
	public FileModificationValidator getFileModificationValidator2() {
		return fileModificationValidator;
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getID()
	 */
	@Override
	public String getID() {
		return PDECore.BINARY_REPOSITORY_PROVIDER;
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getMoveDeleteHook()
	 */
	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return moveDeleteHook;
	}

	private boolean isBinaryResource(IResource resource, boolean excludeProjectChildren) {
		IContainer parent = resource.getParent();

		// Test for resource links
		if (!excludeProjectChildren || !(parent instanceof IProject)) {
			if (resource.isLinked()) {
				return true;
			}
		}

		// Test for resources that are in linked folders

		while (parent instanceof IFolder) {
			IFolder folder = (IFolder) parent;
			if (folder.isLinked()) {
				return true;
			}
			parent = folder.getParent();
		}
		return false;
	}

	private IStatus createProblemStatus() {
		return Status.error(PDECoreMessages.BinaryRepositoryProvider_veto);
	}

	// we need to remove this but our tests will fail if we do, see bug 252003
	@Deprecated
	@Override
	public boolean canHandleLinkedResources() {
		return true;
	}

	public boolean canHandleLinkedResourcesURI() {
		return true;
	}
}
