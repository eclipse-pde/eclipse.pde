/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.team.core.*;
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
	private IPath fTargetPath;

	private IWorkspaceRoot fRoot;
	private IReplaceQuery fReplaceQuery;

	public FeatureImportOperation(
		IFeatureModel[] models,
		IPath targetPath,
		IReplaceQuery replaceQuery) {
		fModels = models;
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
		String name = model.getFeature().getId() + "-feature"; //$NON-NLS-1$
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

			//Mark this project so that we can show image overlay
			// using the label decorator
			project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					PDECore.BINARY_PROJECT_VALUE);
			
				setProjectNatures(project, model, monitor);

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
		if (model.getFeature().getInstallHandler() != null) {
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID, PDE.FEATURE_NATURE });
			IJavaProject jProject = JavaCore.create(project);
			jProject.setRawClasspath(
				new IClasspathEntry[] {
					JavaCore.newContainerEntry(
						new Path("org.eclipse.jdt.launching.JRE_CONTAINER"))}, //$NON-NLS-1$
				monitor);
		} else {
			desc.setNatureIds(new String[] { PDE.FEATURE_NATURE });
		}
		project.setDescription(desc, new SubProgressMonitor(monitor, 1));
	}

}
