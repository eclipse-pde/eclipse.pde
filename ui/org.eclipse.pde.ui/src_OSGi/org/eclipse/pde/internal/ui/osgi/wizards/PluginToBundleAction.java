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
package org.eclipse.pde.internal.ui.osgi.wizards;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.osgi.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.osgi.util.tracker.*;
public class PluginToBundleAction implements IWorkbenchWindowActionDelegate {
	private ISelection fSelection;
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
						&& OSGiWorkspaceModelManager.isPluginProject(project)
						&& !OSGiWorkspaceModelManager.isBundleProject(project)) {
					IPluginModelBase model = findModelFor(project);
					if (model != null && model.isLoaded()
							&& !model.getUnderlyingResource().isLinked()
							&& !models.contains(model)) {
						models.add(model);
					}
				}
			}
			IPluginModelBase[] modelArray = (IPluginModelBase[]) models
					.toArray(new IPluginModelBase[models.size()]);
			PluginToBundleWizard wizard = new PluginToBundleWizard(modelArray);
			WizardDialog dialog = new WizardDialog(PDEPlugin
					.getActiveWorkbenchShell(), wizard);
			dialog.open();
		}
	}
	public static void run(boolean fork, IRunnableContext context,
			final IPluginModelBase[] models) {
		try {
			context.run(fork, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
							public void run(IProgressMonitor monitor)
									throws CoreException {
								doConvertPluginsToBundles(monitor, models);
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
			String title = PDEPlugin
					.getResourceString("PluginToBundleWizard.wtitle");
			String message = PDEPlugin
					.getResourceString("PluginToBundleWizard.error");
			PDEPlugin.logException(e, title, message);
		}
	}
	private IPluginModelBase findModelFor(IProject project) {
		IWorkspaceModelManager manager = PDECore.getDefault()
				.getWorkspaceModelManager();
		return (IPluginModelBase) manager.getWorkspaceModel(project);
	}
	public static void doConvertPluginsToBundles(IProgressMonitor monitor,
			IPluginModelBase[] models) throws CoreException {
		monitor.beginTask(PDEPlugin
				.getResourceString("PluginToBundleWizard.convert"),
				models.length);
		try {
			for (int i = 0; i < models.length; i++) {
				monitor.subTask(models[i].getPluginBase().getId());
				IProject project = models[i].getUnderlyingResource().getProject();
				createBundleManifest(project);
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
				monitor.worked(1);
				if (monitor.isCanceled())
					break;
			}
		} finally {
			monitor.done();
		}
	}

	private static void createBundleManifest(IProject project)
			throws CoreException {
		File outputFile = new File(project.getLocation().append(
				"META-INF/MANIFEST.MF").toOSString());
		File inputFile = new File(project.getLocation().append("plugin.xml")
				.toOSString());
		ServiceTracker tracker = new ServiceTracker(PDEPlugin.getDefault()
				.getBundleContext(), PluginConverter.class.getName(), null);
		tracker.open();
		PluginConverter converter = (PluginConverter) tracker.getService();
		converter.convertManifest(inputFile, outputFile);
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}
}
