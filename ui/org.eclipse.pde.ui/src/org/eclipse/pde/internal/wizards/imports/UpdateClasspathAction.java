/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

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
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.BuildPathUtil;

import org.eclipse.jdt.core.*;

public class UpdateClasspathAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			final ArrayList models = new ArrayList(elems.length);
			final ArrayList projects = new ArrayList(elems.length);

			try {
				for (int i = 0; i < elems.length; i++) {
					if (elems[i] instanceof IFile) {
						IFile file = (IFile) elems[i];
						IProject project = file.getProject();
						if (project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
							IPluginModelBase model = findModelFor(project);
							if (model != null) {
								models.add(model);
								projects.add(JavaCore.create(project));
							}
						}
					}
				}

				ProgressMonitorDialog dialog =
					new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
				dialog.run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
						try {
							IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
								public void run(IProgressMonitor monitor) throws CoreException {
									doUpdateClasspath(monitor, models, projects);
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
				String title = "Update Classpaths";
				String message = "Updating failed. See log for details.";
				PDEPlugin.logException(e, title, message);
			} catch (CoreException e) {
				String title = "Update Classpaths";
				String message = "Updating failed. See log for details.";
				PDEPlugin.logException(e, title, message);
			}
		}
	}

	private IPluginModelBase findModelFor(IProject project) {
		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceModel(project);
	}

	private void doUpdateClasspath(
		IProgressMonitor monitor,
		ArrayList models,
		ArrayList projects)
		throws CoreException {
		monitor.beginTask("Update classpaths...", models.size());
		try {
			for (int i = 0; i < models.size(); i++) {
				IPluginModelBase model = models[i];
				BuildPathUtil.setBuildPath(model, monitor);
				monitor.worked(1);
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						}
					}
				};
				try {
	   				monitor.run(false, false, op);
				}
				catch (InterruptedException e) {
					PDEPlugin.logException(e);
				}
				catch (InvocationTargetException e) {
					PDEPlugin.logException(e.getTargetException());
				}
			}
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