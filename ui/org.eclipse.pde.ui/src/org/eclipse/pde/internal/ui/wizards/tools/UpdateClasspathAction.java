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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.*;

public class UpdateClasspathAction implements IViewActionDelegate {
	private ISelection fSelection;
	private static final String KEY_TITLE = "Actions.classpath.title"; //$NON-NLS-1$
	private static final String KEY_MESSAGE = "Actions.classpath.message"; //$NON-NLS-1$
	private static final String KEY_UPDATE = "Actions.classpath.update"; //$NON-NLS-1$

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			ArrayList models = new ArrayList(elems.length);

			for (int i = 0; i < elems.length; i++) {
				Object elem = elems[i];
				IProject project = null;

				if (elem instanceof IFile) {
					IFile file = (IFile) elem;
					project = file.getProject();
				} else if (elem instanceof IProject) {
					project = (IProject) elem;
				} else if (elem instanceof IJavaProject) {
					project = ((IJavaProject) elem).getProject();
				}
				if (project != null
				 && WorkspaceModelManager.isJavaPluginProject(project)) {
					IPluginModelBase model = findModelFor(project);
					if (model != null) {
						models.add(model);
					}
				}
			}

			final IPluginModelBase[] modelArray =
				(IPluginModelBase[]) models.toArray(
					new IPluginModelBase[models.size()]);
			/*
			 * ProgressMonitorDialog pd = new
			 * ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			 * run(true, pd, modelArray);
			 */
			UpdateBuildpathWizard wizard =
				new UpdateBuildpathWizard(modelArray);
			final WizardDialog dialog =
				new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			BusyIndicator
				.showWhile(
					PDEPlugin.getActiveWorkbenchShell().getDisplay(),
					new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
	}

	public static void run(
		boolean fork,
		IRunnableContext context,
		final IPluginModelBase[] models) {
		try {
			context.run(fork, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
					try {
						IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
							public void run(IProgressMonitor monitor)
								throws CoreException {
								doUpdateClasspath(monitor, models);
							}
						};
						PDEPlugin.getWorkspace().run(runnable, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException(e.getMessage());
					}
				}
			});
		} catch (InterruptedException e) {
			return;
		} catch (InvocationTargetException e) {
			String title = PDEPlugin.getResourceString(KEY_TITLE);
			String message = PDEPlugin.getResourceString(KEY_MESSAGE);
			PDEPlugin.logException(e, title, message);
		}
	}

	private IPluginModelBase findModelFor(IProject project) {
		WorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		return (IPluginModelBase) manager.getWorkspaceModel(project);
	}
	
	public static void doUpdateClasspath(
		IProgressMonitor monitor,
		IPluginModelBase[] models)
		throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATE), models.length);
		try {
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				monitor.subTask(models[i].getPluginBase().getId());
				// no reason to compile classpath for a non-Java model
				IProject project = model.getUnderlyingResource().getProject();
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					monitor.worked(1);
					continue;
				}
				ClasspathUtilCore.setClasspath(
					model,
					new SubProgressMonitor(monitor, 1));
				if (monitor.isCanceled())
					break;
			}
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IViewPart view) {
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

}
