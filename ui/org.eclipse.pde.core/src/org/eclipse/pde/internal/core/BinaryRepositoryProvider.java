package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class BinaryRepositoryProvider extends RepositoryProvider {
	private IMoveDeleteHook moveDeleteHook;
	private IFileModificationValidator fileModificationValidator;
	class BinaryMoveDeleteHook implements IMoveDeleteHook {
		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteFile(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFile, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean deleteFile(
			IResourceTree tree,
			IFile file,
			int updateFlags,
			IProgressMonitor monitor) {
			if (isBinaryResource(file))
				tree.failed(createProblemStatus());
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteFolder(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFolder, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean deleteFolder(
			IResourceTree tree,
			IFolder folder,
			int updateFlags,
			IProgressMonitor monitor) {
			if (isBinaryResource(folder))
				tree.failed(createProblemStatus());
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#deleteProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean deleteProject(
			IResourceTree tree,
			IProject project,
			int updateFlags,
			IProgressMonitor monitor) {
			return false;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveFile(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFile, org.eclipse.core.resources.IFile, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean moveFile(
			IResourceTree tree,
			IFile source,
			IFile destination,
			int updateFlags,
			IProgressMonitor monitor) {
			if (isBinaryResource(source))
				tree.failed(createProblemStatus());
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveFolder(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IFolder, org.eclipse.core.resources.IFolder, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean moveFolder(
			IResourceTree tree,
			IFolder source,
			IFolder destination,
			int updateFlags,
			IProgressMonitor monitor) {
			if (isBinaryResource(source))
				tree.failed(createProblemStatus());
			return true;
		}

		/**
		 * @see org.eclipse.core.resources.team.IMoveDeleteHook#moveProject(org.eclipse.core.resources.team.IResourceTree, org.eclipse.core.resources.IProject, org.eclipse.core.resources.IProjectDescription, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public boolean moveProject(
			IResourceTree tree,
			IProject source,
			IProjectDescription description,
			int updateFlags,
			IProgressMonitor monitor) {
			return false;
		}
	}

	class BinaryFileModificationValidator
		implements IFileModificationValidator {
		/**
		 * @see org.eclipse.core.resources.IFileModificationValidator#validateEdit(org.eclipse.core.resources.IFile, java.lang.Object)
		 */
		public IStatus validateEdit(IFile[] files, Object context) {
			for (int i = 0; i < files.length; i++) {
				if (isBinaryResource(files[i])) {
					return createProblemStatus();
				}
			}
			return createOKStatus();
		}

		/**
		 * @see org.eclipse.core.resources.IFileModificationValidator#validateSave(org.eclipse.core.resources.IFile)
		 */
		public IStatus validateSave(IFile file) {
			if (isBinaryResource(file)) {
				return createProblemStatus();
			}
			return createOKStatus();
		}
	}

	public BinaryRepositoryProvider() {
		moveDeleteHook = new BinaryMoveDeleteHook();
		fileModificationValidator = new BinaryFileModificationValidator();
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	public void configureProject() throws CoreException {
		IProject project = getProject();
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.EXTERNAL_PROJECT_VALUE);
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProject project = getProject();
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, null);
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getFileModificationValidator()
	 */
	public IFileModificationValidator getFileModificationValidator() {
		return fileModificationValidator;
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getID()
	 */
	public String getID() {
		return PDECore.BINARY_REPOSITORY_PROVIDER;
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getMoveDeleteHook()
	 */
	public IMoveDeleteHook getMoveDeleteHook() {
		return moveDeleteHook;
	}

	private boolean isBinaryResource(IResource resource) {
		if (resource.isLinked())
			return true;

		IContainer parent = resource.getParent();

		while (parent instanceof IFolder) {
			IFolder folder = (IFolder) parent;
			if (folder.isLinked())
				return true;
			parent = folder.getParent();
		}
		return false;
	}

	private IStatus createProblemStatus() {
		String message = PDECore.getResourceString("BinaryRepositoryProvider.veto"); //$NON-NLS-1$
		return new Status(
			IStatus.ERROR,
			PDECore.PLUGIN_ID,
			IStatus.OK,
			message,
			null);
	}

	private IStatus createOKStatus() {
		return new Status(IStatus.OK, PDECore.PLUGIN_ID, IStatus.OK, "", //$NON-NLS-1$
		null);
	}
}
