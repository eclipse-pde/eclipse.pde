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

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * @author cgwong
 */
public class ConvertProjectsAction implements IObjectActionDelegate {

	private ISelection fSelection;
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (!hasProjectsToConvert()){
			MessageDialog dialog = new MessageDialog(this.getDisplay().getActiveShell(), PDEPlugin.getResourceString("ConvertProjectsAction.find"), //$NON-NLS-1$
					null, PDEPlugin.getResourceString("ConvertProjectsAction.none"), //$NON-NLS-1$
					MessageDialog.INFORMATION, new String[]{IDialogConstants.OK_LABEL}, 0);
			dialog.open();
			return;
		}
		
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			Vector projects = new Vector(elems.length);

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
				if (project != null)
					projects.add(project);
			}
			
		ConvertedProjectWizard wizard = new ConvertedProjectWizard(projects);
		
		final Display display = getDisplay();
		final WizardDialog dialog =
			new WizardDialog(display.getActiveShell(), wizard);
		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}
	
	public Display getDisplay(){
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}
	
	private boolean hasProjectsToConvert(){
		IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i<projects.length; i++){
			if (projects[i].isOpen() && !PDE.hasPluginNature(projects[i]))
				return true;
		}
		return false;
	}
}
