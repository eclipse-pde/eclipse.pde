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
import org.eclipse.jface.util.Assert;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.team.core.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;


public class FeatureImportOperation implements IWorkspaceRunnable {
	private static final String KEY_CREATING =
		"FeatureImportWizard.operation.creating";
	private static final String KEY_CREATING2 =
		"FeatureImportWizard.operation.creating2";
	private static final String KEY_MULTI_PROBLEM =
		"FeatureImportWizard.operation.multiProblem";

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

	private IFeatureModel[] models;
	private IPath targetPath;

	private IWorkspaceRoot root;
	private IReplaceQuery replaceQuery;

	public FeatureImportOperation(
		IFeatureModel[] models,
		IPath targetPath,
		IReplaceQuery replaceQuery) {
		Assert.isNotNull(models);
		Assert.isNotNull(replaceQuery);
		this.models = models;
		this.targetPath = targetPath;

		root = ResourcesPlugin.getWorkspace().getRoot();
		this.replaceQuery = replaceQuery;
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
			PDEPlugin.getResourceString(KEY_CREATING),
			models.length);
		try {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString(KEY_MULTI_PROBLEM),
					null);
			//long start = System.currentTimeMillis();
			for (int i = 0; i < models.length; i++) {
				try {
					createProject(
						models[i],
						new SubProgressMonitor(monitor, 1));
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
			//long stop = System.currentTimeMillis();
			//System.out.println("Import time: "+(stop-start)+"ms");
		} finally {
			monitor.done();
		}
	}

	private void createProject(IFeatureModel model, IProgressMonitor monitor)
		throws CoreException {
		String name = model.getFeature().getId() + "-feature";
		String task = PDEPlugin.getFormattedMessage(KEY_CREATING2, name);
		monitor.beginTask(task, 8);
		try {
			IProject project = root.getProject(name);

			if (project.exists()) {
				if (queryReplace(project)) {
					project.delete(
						true,
						true,
						new SubProgressMonitor(monitor, 1));
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
			
			IProjectDescription description = PDEPlugin.getWorkspace().newProjectDescription(name);
			description.setLocation(targetPath.append(name));

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

			setProjectNatures(project, monitor);

			//Mark this project so that we can show image overlay
			// using the label decorator
			project.setPersistentProperty(
				PDECore.EXTERNAL_PROJECT_PROPERTY,
				PDECore.BINARY_PROJECT_VALUE);

			IJavaProject jProject = JavaCore.create(project);
			jProject.setRawClasspath(computeClasspath(), monitor);
		} finally {
			monitor.done();
		}
	}

	private IClasspathEntry[] computeClasspath() {
		IClasspathEntry[] entries = new IClasspathEntry[1];
		IPath jrePath = new Path("JRE_LIB");
		IPath[] annot = new IPath[2];
		annot[0] = new Path("JRE_SRC");
		annot[1] = new Path("JRE_SRCROOT");
		entries[0] = JavaCore.newVariableEntry(jrePath, annot[0], annot[1]);
		return entries;
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
		ImportOperation op =
			new ImportOperation(destPath, source, provider, query);
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

	/*private String getFlatPath(IPath path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.segmentCount(); i++) {
			if (i > 0)
				buf.append("_");
			buf.append(path.segment(i));
		}
		return buf.toString();
	}*/

	private boolean queryReplace(IProject project)
		throws OperationCanceledException {
		switch (replaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL :
				throw new OperationCanceledException();
			case IReplaceQuery.NO :
				return false;
		}
		return true;
	}

	private void setProjectNatures(
		IProject project,
		IProgressMonitor monitor)
		throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(
			new String[] { JavaCore.NATURE_ID, PDE.FEATURE_NATURE });
		project.setDescription(desc, new SubProgressMonitor(monitor, 1));
	}

}
