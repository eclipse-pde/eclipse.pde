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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class UpdateClasspathAction implements IViewActionDelegate {
	private ISelection fSelection;

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IPluginModelBase[] fUnupdated = getModelsToUpdate();
		if (fUnupdated.length == 0) {
			MessageDialog.openInformation(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("UpdateClasspathAction.find"),  //$NON-NLS-1$
					PDEPlugin.getResourceString("UpdateClasspathAction.none")); //$NON-NLS-1$
			return;
		}
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			ArrayList models = new ArrayList(elems.length);
			PluginModelManager manager = PDECore.getDefault().getModelManager();
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
					IPluginModelBase model = manager.findModel(project);
					if (model != null) {
						models.add(model);
					}
				}
			}

			final IPluginModelBase[] modelArray = (IPluginModelBase[]) models
					.toArray(new IPluginModelBase[models.size()]);

			UpdateBuildpathWizard wizard = new UpdateBuildpathWizard(fUnupdated, modelArray);
			final WizardDialog dialog = new WizardDialog(PDEPlugin
					.getActiveWorkbenchShell(), wizard);
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell()
					.getDisplay(), new Runnable() {
				public void run() {
					dialog.open();
				}
			});
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
	
	private IPluginModelBase[] getModelsToUpdate(){
		IPluginModelBase[] models =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		ArrayList modelArray = new ArrayList();
		try{
			for (int i = 0; i < models.length; i++) {
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(models[i]);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return (IPluginModelBase[])modelArray.toArray(new IPluginModelBase[modelArray.size()]);
	}
	
}