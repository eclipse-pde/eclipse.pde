/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to update the bundle classpath for the selected projects
 *
 */
public class UpdateClasspathAction extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPluginModelBase[] fUnupdated = getModelsToUpdate();
		if (fUnupdated.length == 0) {
			MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.UpdateClasspathAction_find, PDEUIMessages.UpdateClasspathAction_none);
			return null;
		}
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) selection).toArray();
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
				try {
					if (project != null && WorkspaceModelManager.isPluginProject(project) && project.hasNature(JavaCore.NATURE_ID)) {
						IPluginModelBase model = PluginRegistry.findModel(project);
						if (model != null) {
							models.add(model);
						}
					}
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}

			final IPluginModelBase[] modelArray = (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);

			UpdateBuildpathWizard wizard = new UpdateBuildpathWizard(fUnupdated, modelArray);
			final WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
		return null;
	}

	private IPluginModelBase[] getModelsToUpdate() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		ArrayList modelArray = new ArrayList();
		try {
			for (int i = 0; i < models.length; i++) {
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(models[i]);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return (IPluginModelBase[]) modelArray.toArray(new IPluginModelBase[modelArray.size()]);
	}

}
