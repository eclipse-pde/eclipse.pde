/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
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
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.natures.SiteProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ConvertProjectsAction implements IObjectActionDelegate {

	private ISelection fSelection;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		IProject[] unconverted = getUnconvertedProjects();
		if (unconverted.length == 0) {
			MessageDialog.openInformation(this.getDisplay().getActiveShell(), PDEUIMessages.ConvertProjectsAction_find, PDEUIMessages.ConvertProjectsAction_none); //
			return;
		}

		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			Vector<IProject> initialSelection = new Vector<>(elems.length);

			for (Object elem : elems) {
				IProject project = null;

				if (elem instanceof IFile file) {
					project = file.getProject();
				} else if (elem instanceof IProject) {
					project = (IProject) elem;
				} else if (elem instanceof IJavaProject) {
					project = ((IJavaProject) elem).getProject();
				}
				if (project != null)
					initialSelection.add(project);
			}

			ConvertedProjectWizard wizard = new ConvertedProjectWizard(unconverted, initialSelection);

			final Display display = getDisplay();
			final WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
			BusyIndicator.showWhile(display, () -> dialog.open());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	public Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	private IProject[] getUnconvertedProjects() {
		ArrayList<IProject> unconverted = new ArrayList<>();
		IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (project.isOpen() && !PluginProject.isPluginProject(project) && !FeatureProject.isFeatureProject(project)
					&& !SiteProject.isSiteProject(project) && project.getName().indexOf('%') == -1
					&& project.getLocation().toString().indexOf('%') == -1)
				unconverted.add(project);
		}
		return unconverted.toArray(new IProject[unconverted.size()]);
	}
}
