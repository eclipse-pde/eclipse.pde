/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Allows quick access to the problem filters property page for removal of API
 * problem filters
 * 
 * @since 1.0.0
 */
public class ConfigureProblemFiltersAction implements IObjectActionDelegate {

	private ISelection selection = null;
	
	/**
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		IAdaptable element = getAdaptable(this.selection);
		if(element == null) {
			return;
		}
		SWTFactory.showPropertiesDialog(ApiUIPlugin.getShell(),
				IApiToolsConstants.ID_FILTERS_PROP_PAGE,
				element, 
				null);
	}

	/**
	 * Returns the {@link IAdaptable} from the current selection context
	 * @param selection
	 * @return the {@link IAdaptable} for the current selection context
	 */
	private IAdaptable getAdaptable(ISelection selection) {
		if(selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object o = ss.getFirstElement();
			if(o instanceof IAdaptable) {
				IAdaptable adapt = (IAdaptable) o;
				IResource resource = (IResource) adapt.getAdapter(IResource.class);
				if(resource != null) {
					return (resource instanceof IProject ? resource : resource.getProject());
				}
			}
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

}
