/*******************************************************************************
 *  Copyright (c) 2018, 2024 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.CreateModuleInfoAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;

@SuppressWarnings("restriction")
public class CreateModuleInfoPDEAction extends CreateModuleInfoAction {
	private ISelection fSelection;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		fSelection = selection;
	}

	private Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	@Override
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection structuredSelection) {
			Object selectedElement = structuredSelection.getFirstElement();

			IProject project;
			if (selectedElement instanceof IProject iProject) {
				project = iProject;
			} else if (selectedElement instanceof IJavaProject javaProject) {
				project = javaProject.getProject();
			} else {
				return;
			}
			if (PluginProject.isPluginProject(project) && !MessageDialog.openConfirm(getDisplay().getActiveShell(),
					PDEUIMessages.CreateModuleInfoPDEAction_sync_issue,
					PDEUIMessages.CreateModuleInfoPDEAction_mod_info_not_in_sync)) {
				return;
			}
		}
		super.run(action);
	}
}
