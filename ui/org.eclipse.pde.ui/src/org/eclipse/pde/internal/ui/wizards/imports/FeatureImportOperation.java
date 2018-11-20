/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

public class FeatureImportOperation implements IWorkspaceRunnable {
	public interface IReplaceQuery {

		// return codes
		public static final int CANCEL = 0;
		public static final int NO = 1;
		public static final int YES = 2;

		/**
		 * Do the callback. Returns YES, NO or CANCEL
		 */
		int doQuery(IProject project);
	}

	private IFeatureModel[] fModels;
	private boolean fBinary;
	private IPath fTargetPath;

	private IWorkspaceRoot fRoot;
	private IReplaceQuery fReplaceQuery;

	/**
	 *
	 * @param models
	 * @param targetPath a parent of external project or null
	 * @param replaceQuery
	 */
	public FeatureImportOperation(IFeatureModel[] models, boolean binary, IPath targetPath, IReplaceQuery replaceQuery) {
		fModels = models;
		fBinary = binary;
		fTargetPath = targetPath;
		fRoot = ResourcesPlugin.getWorkspace().getRoot();
		fReplaceQuery = replaceQuery;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDEUIMessages.FeatureImportWizard_operation_creating,
				fModels.length);
		MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(), IStatus.OK,
				PDEUIMessages.FeatureImportWizard_operation_multiProblem, null);
		for (IFeatureModel model : fModels) {
			try {
				createProject(model, subMonitor.split(1));
			} catch (CoreException e) {
				multiStatus.merge(e.getStatus());
			}
			if (subMonitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
		if (!multiStatus.isOK()) {
			throw new CoreException(multiStatus);
		}
	}

	private void createProject(IFeatureModel model, IProgressMonitor monitor) throws CoreException {
		String name = model.getFeature().getId();

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		for (IFeaturePlugin plugin : plugins) {
			if (name.equals(plugin.getId())) {
				name += "-feature"; //$NON-NLS-1$
				break;
			}

		}

		SubMonitor subMonitor = SubMonitor.convert(monitor,
				NLS.bind(PDEUIMessages.FeatureImportWizard_operation_creating2, name), 9);
		IProject project = fRoot.getProject(name);

		if (project.exists() || new File(project.getParent().getLocation().toFile(), name).exists()) {
			if (queryReplace(project)) {
				if (!project.exists()) {
					project.create(subMonitor.split(1));
				}
				project.delete(true, true, subMonitor.split(1));
				try {
					RepositoryProvider.unmap(project);
				} catch (TeamException e) {
				}
			} else {
				return;
			}
		}
		subMonitor.setWorkRemaining(7);

		IProjectDescription description = PDEPlugin.getWorkspace().newProjectDescription(name);
		if (fTargetPath != null)
			description.setLocation(fTargetPath.append(name));

		project.create(description, subMonitor.split(1));
		if (!project.isOpen()) {
			project.open(null);
		}
		File featureDir = new File(model.getInstallLocation());

		importContent(featureDir, project.getFullPath(), FileSystemStructureProvider.INSTANCE, null,
				subMonitor.split(1));
		IFolder folder = project.getFolder("META-INF"); //$NON-NLS-1$
		if (folder.exists()) {
			folder.delete(true, null);
		}
		if (fBinary) {
			// Mark this project so that we can show image overlay
			// using the label decorator
			project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);
		}
		createBuildProperties(project);
		setProjectNatures(project, model, subMonitor.split(1));
		if (project.hasNature(JavaCore.NATURE_ID)) {
			setClasspath(project, model, subMonitor.split(4));
		}
	}

	private void importContent(Object source, IPath destPath, IImportStructureProvider provider, List<?> filesToImport, IProgressMonitor monitor) throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
			@Override
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		ImportOperation op = new ImportOperation(destPath, source, provider, query);
		op.setCreateContainerStructure(false);
		if (filesToImport != null) {
			op.setFilesToImport(filesToImport);
		}

		try {
			op.run(monitor);
		} catch (InvocationTargetException e) {
			Throwable th = e.getTargetException();
			if (th instanceof CoreException) {
				throw (CoreException) th;
			}
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}

	private boolean queryReplace(IProject project) throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL :
				throw new OperationCanceledException();
			case IReplaceQuery.NO :
				return false;
		}
		return true;
	}

	private void setProjectNatures(IProject project, IFeatureModel model, SubMonitor subMonitor) throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature(model)) {
			desc.setNatureIds(new String[] {JavaCore.NATURE_ID, PDE.FEATURE_NATURE});
		} else {
			desc.setNatureIds(new String[] {PDE.FEATURE_NATURE});
		}
		subMonitor.setWorkRemaining(1);
		project.setDescription(desc, subMonitor.split(1));
	}

	private void setClasspath(IProject project, IFeatureModel model, IProgressMonitor monitor)
			throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		IJavaProject jProject = JavaCore.create(project);

		IClasspathEntry jreCPEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$

		String libName = model.getFeature().getInstallHandler().getLibrary();
		IClasspathEntry handlerCPEntry = JavaCore.newLibraryEntry(project.getFullPath().append(libName), null, null);

		jProject.setRawClasspath(new IClasspathEntry[] { jreCPEntry, handlerCPEntry }, subMonitor);
	}

	private boolean needsJavaNature(IFeatureModel model) {
		IFeatureInstallHandler handler = model.getFeature().getInstallHandler();
		if (handler == null) {
			return false;
		}
		String libName = handler.getLibrary();
		if (libName == null || libName.length() <= 0) {
			return false;
		}
		File lib = new File(model.getInstallLocation(), libName);
		return lib.exists();
	}

	private void createBuildProperties(IProject project) {
		IFile file = PDEProject.getBuildProperties(project);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry = model.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			try {
				IResource[] res = project.members();
				for (IResource resource : res) {
					String path = resource.getProjectRelativePath().toString();
					if (!path.equals(".project")) //$NON-NLS-1$
						ientry.addToken(path);
				}
				model.getBuild().add(ientry);
				model.save();
			} catch (CoreException e) {
			}
		}
	}

}
