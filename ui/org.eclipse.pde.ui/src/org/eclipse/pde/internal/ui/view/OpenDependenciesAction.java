/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
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
			IModel model =
				PDECore
					.getDefault()
					.getWorkspaceModelManager()
					.getWorkspaceModel(
					(IProject) el);
			if (model instanceof IPluginModelBase)
				el = model;
		}
		if (el instanceof IPluginObject) {
			el = ((IPluginObject)el).getModel();
		}
		if (el instanceof IPluginModelBase) {
			openDependencies((IPluginModelBase)el);
		}
	}
	
	private void openDependencies(IPluginModelBase model) {
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