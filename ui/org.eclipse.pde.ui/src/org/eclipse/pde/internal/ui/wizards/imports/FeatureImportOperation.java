/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

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
	public FeatureImportOperation(
		IFeatureModel[] models,
		boolean binary,
		IPath targetPath,
		IReplaceQuery replaceQuery) {
		fModels = models;
		fBinary = binary;
		fTargetPath = targetPath;
		fRoot = ResourcesPlugin.getWorkspace().getRoot();
		fReplaceQuery = replaceQuery;
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor)
		throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(
			PDEPlugin.getResourceString("FeatureImportWizard.operation.creating"), //$NON-NLS-1$
			fModels.length);
		try {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString(
						"FeatureImportWizard.operation.multiProblem"), //$NON-NLS-1$
					null);
			for (int i = 0; i < fModels.length; i++) {
				try {
					createProject(fModels[i], new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			if (!multiStatus.isOK()) {
				throw new CoreException(multiStatus);
			}
		} finally {
			monitor.done();
		}
	}

	private void createProject(IFeatureModel model, IProgressMonitor monitor)
		throws CoreException {
		String name = model.getFeature().getId();

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			if (name.equals(plugins[i].getId())) {
				name += "-feature"; //$NON-NLS-1$
				break;
			}

		}
		
		String task =
			PDEPlugin.getFormattedMessage(
				"FeatureImportWizard.operation.creating2", //$NON-NLS-1$
				name);
		monitor.beginTask(task, 8);
		try {
			IProject project = fRoot.getProject(name);

			if (project.exists()) {
				if (queryReplace(project)) {
					project.delete(true, true, new SubProgressMonitor(monitor, 1));
					try {
						RepositoryProvider.unmap(project);
					} catch (TeamException e) {
					}
				} else {
					return;
				}
			} else {
				monitor.worked(1);
			}

			IProjectDescription description =
				PDEPlugin.getWorkspace().newProjectDescription(name);
			if (fTargetPath != null)
				description.setLocation(fTargetPath.append(name));

			project.create(description, new SubProgressMonitor(monitor, 1));
			if (!project.isOpen()) {
				project.open(null);
			}
			File featureDir = new File(model.getInstallLocation());

			importContent(
				featureDir,
				project.getFullPath(),
				FileSystemStructureProvider.INSTANCE,
				null,
				new SubProgressMonitor(monitor, 1));

			if (fBinary) {
				// Mark this project so that we can show image overlay
				// using the label decorator
				project.setPersistentProperty(
						PDECore.EXTERNAL_PROJECT_PROPERTY,
						PDECore.BINARY_PROJECT_VALUE);
			}
			
				setProjectNatures(project, model, monitor);
				if (project.hasNature(JavaCore.NATURE_ID))
					setClasspath(project, model, monitor);

		} finally {
			monitor.done();
		}
	}

	private void importContent(
		Object source,
		IPath destPath,
		IImportStructureProvider provider,
		List filesToImport,
		IProgressMonitor monitor)
		throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
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
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
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

	private void setProjectNatures(
		IProject project,
		IFeatureModel model,
		IProgressMonitor monitor)
		throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature(model)) {
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID,
					PDE.FEATURE_NATURE });
		} else {
			desc.setNatureIds(new String[] { PDE.FEATURE_NATURE });
		}
		project.setDescription(desc, new SubProgressMonitor(monitor, 1));
	}

	private void setClasspath(IProject project, IFeatureModel model,
			IProgressMonitor monitor) throws JavaModelException {
		IJavaProject jProject = JavaCore.create(project);

		IClasspathEntry jreCPEntry = JavaCore.newContainerEntry(new Path(
				"org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$

		String libName = model.getFeature().getInstallHandler().getLibrary();
		IClasspathEntry handlerCPEntry = JavaCore.newLibraryEntry(project
				.getFullPath().append(libName), null, null);

		jProject.setRawClasspath(new IClasspathEntry[] { jreCPEntry,
				handlerCPEntry }, monitor);
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

}
