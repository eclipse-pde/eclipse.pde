/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.ui.BuildPathUtil;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.pde.internal.ui.preferences.BuildpathPreferencePage;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.internal.core.*;

import org.eclipse.jdt.core.*;

public class UpdateClasspathAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	private static final String KEY_TITLE = "Actions.classpath.title";
	private static final String KEY_MESSAGE = "Actions.classpath.message";
	private static final String KEY_UPDATE = "Actions.classpath.update";
	private static final String KEY_SETTING = "Actions.classpath.setting";
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
				if (project != null && WorkspaceModelManager.isJavaPluginProject(project)) {
					IPluginModelBase model = findModelFor(project);
					if (model != null) {
						models.add(model);
					}
				}
			}

			final IPluginModelBase[] modelArray =
				(IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
			/*
			ProgressMonitorDialog pd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			run(true, pd, modelArray);
			*/
			UpdateBuildpathWizard wizard = new UpdateBuildpathWizard(modelArray);
			WizardDialog dialog =
				new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.open();
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
							public void run(IProgressMonitor monitor) throws CoreException {
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
		return (IPluginModelBase)manager.getWorkspaceModel(project);
	}

	private static void doUpdateClasspath(
		IProgressMonitor monitor,
		IPluginModelBase[] models)
		throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATE), models.length);
		boolean useContainers = BuildpathPreferencePage.getUseClasspathContainers();
		try {
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				setProjectBuildpath(model, useContainers, subMonitor);
				if (monitor.isCanceled()) break;
			}
		} finally {
			monitor.done();
		}
	}

	private static void setProjectBuildpath(
		IPluginModelBase model,
		boolean useContainers,
		IProgressMonitor monitor)
		throws CoreException {
		IPluginBase pluginBase = model.getPluginBase();
		String message = PDEPlugin.getFormattedMessage(KEY_SETTING, pluginBase.getId());
		monitor.beginTask(message, 1);
		try {
			BuildPathUtil.setBuildPath(model, useContainers, monitor);
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
	public void init(IWorkbenchWindow window) {
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

}