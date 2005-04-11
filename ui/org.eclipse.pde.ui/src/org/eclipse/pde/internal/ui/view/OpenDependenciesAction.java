/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;

public class OpenDependenciesAction implements IWorkbenchWindowActionDelegate {
	private ISelection fSelection;
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			openDependencies(ssel.getFirstElement());
		}
	}

	private void openDependencies(Object el) {
		if (el instanceof IFile) {
			el = ((IFile)el).getProject();
		}
		if (el instanceof IJavaProject) {
			el = ((IJavaProject)el).getProject();
		}
		if (el instanceof IProject) {
			el = PDECore.getDefault().getModelManager().findModel((IProject) el);
		}
		if (el instanceof IPluginObject) {
			el = ((IPluginObject)el).getModel();
		}
		if (el instanceof IPluginModelBase) {
			openDependencies((IPluginModelBase)el);
		}
	}
	
	public static void openDependencies(IPluginModelBase model) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		try {
			IViewPart view = page.showView(PDEPlugin.DEPENDENCIES_VIEW_ID);
			((DependenciesView)view).openTo(model);
		}
		catch (PartInitException e) {
			PDEPlugin.logException(e);
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
