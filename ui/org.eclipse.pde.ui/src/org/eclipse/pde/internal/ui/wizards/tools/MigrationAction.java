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

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

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
							PDEUIMessages.MigrationAction_find, PDEUIMessages.MigrationAction_none);//$NON-NLS-1$ //$NON-NLS-2$
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
					&& models[i].getPluginBase().getSchemaVersion() == null)
				result.add(models[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result
				.size()]);
	}

}
