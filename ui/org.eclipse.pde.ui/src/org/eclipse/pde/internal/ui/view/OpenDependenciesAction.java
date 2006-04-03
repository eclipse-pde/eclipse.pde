/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

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
			IViewPart view = page.showView(IPDEUIConstants.DEPENDENCIES_VIEW_ID);
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
