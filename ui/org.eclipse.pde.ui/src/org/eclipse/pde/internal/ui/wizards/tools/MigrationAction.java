/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class MigrationAction implements IObjectActionDelegate {

	private ISelection fSelection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		IPluginModelBase[] modelsToMigrate = getModelsToMigrate();
		if (modelsToMigrate.length == 0) {
			MessageDialog
					.openInformation(
							this.getDisplay().getActiveShell(),
							PDEUIMessages.MigrationAction_find, PDEUIMessages.MigrationAction_none);// 
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
				if (project != null) {
					IPluginModelBase model = manager.findModel(project);
					if (model != null) {
						models.add(model);
					}
				}
			}

			final IPluginModelBase[] modelArray = (IPluginModelBase[]) models
					.toArray(new IPluginModelBase[models.size()]);

			MigratePluginWizard wizard = new MigratePluginWizard(
					modelsToMigrate, modelArray);
			final Display display = getDisplay();
			final WizardDialog dialog = new WizardDialog(display
					.getActiveShell(), wizard);
			BusyIndicator.showWhile(display, new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	private Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	private IPluginModelBase[] getModelsToMigrate() {
		Vector result = new Vector();
		IPluginModelBase[] models = PDECore.getDefault()
				.getModelManager().getWorkspaceModels();
		for (int i = 0; i < models.length; i++) {
			if (!models[i].getUnderlyingResource().isLinked()
					&& models[i].isLoaded()
					&& !(models[i] instanceof IBundlePluginModelBase)
					&& models[i].getPluginBase().getSchemaVersion() == null)
				result.add(models[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result
				.size()]);
	}

}
