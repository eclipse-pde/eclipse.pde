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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.ui.ClasspathUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class UpdateClasspathAction implements IViewActionDelegate {
	private ISelection fSelection;
	private static final String KEY_TITLE = "Actions.classpath.title"; //$NON-NLS-1$
	private static final String KEY_MESSAGE = "Actions.classpath.message"; //$NON-NLS-1$
	private static final String KEY_UPDATE = "Actions.classpath.update"; //$NON-NLS-1$
	private static final String KEY_SETTING = "Actions.classpath.setting"; //$NON-NLS-1$

	public static class MissingPluginConfirmation
		implements IMissingPluginConfirmation {
		private boolean useProjectReference = false;
		private boolean alreadyAsked = false;
		public boolean getUseProjectReference() {
			if (!alreadyAsked) {
				useProjectReference = syncAsk();
				alreadyAsked = true;
			}
			return useProjectReference;
		}
		private boolean syncAsk() {
			Display display = SWTUtil.getStandardDisplay();
			final Shell shell = PDEPlugin.getActiveWorkbenchShell();
			final boolean[] result = new boolean[1];
			display.syncExec(new Runnable() {
				public void run() {
						result[0] = MessageDialog.openQuestion(shell, PDEPlugin.getResourceString("UpdateClasspathAction.missingPlugin.title"), //$NON-NLS-1$
	PDEPlugin.getResourceString("UpdateClasspathAction.missingPlugin.message")); //$NON-NLS-1$
				}
			});
			return result[0];
		}
	}

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
						IWorkspaceRunnable runnable =
							new IWorkspaceRunnable() {
							public void run(IProgressMonitor monitor)
								throws CoreException {
								doUpdateClasspath(monitor, models, null);
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
		IWorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		return (IPluginModelBase) manager.getWorkspaceModel(project);
	}

	public static void doUpdateClasspath(
		IProgressMonitor monitor,
		IPluginModelBase[] models,
		MissingPluginConfirmation confirmation)
		throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_UPDATE),
			models.length);
		boolean useContainers = PDEPlugin.getUseClasspathContainers();
		try {
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				// no reason to compile classpath for a non-Java model
				IProject project = model.getUnderlyingResource().getProject();
				if (!project.hasNature(JavaCore.NATURE_ID))
					continue;
				IProgressMonitor subMonitor =
					new SubProgressMonitor(monitor, 1);
				setProjectBuildpath(
					model,
					useContainers,
					confirmation,
					subMonitor);
				if (monitor.isCanceled())
					break;
			}
		} finally {
			monitor.done();
		}
	}

	private static void setProjectBuildpath(
		IPluginModelBase model,
		boolean useContainers,
		IMissingPluginConfirmation confirmation,
		IProgressMonitor monitor)
		throws CoreException {
		IPluginBase pluginBase = model.getPluginBase();
		String message =
			PDEPlugin.getFormattedMessage(KEY_SETTING, pluginBase.getId());
		monitor.beginTask(message, 1);
		try {
			ClasspathUtil.setClasspath(
				model,
				useContainers,
				confirmation,
				monitor);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
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
