/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FindPluginReferencesAction implements IObjectActionDelegate {
	private String searchString = null;
	/**
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (searchString == null)
			return;
		PluginSearchInput input = new PluginSearchInput();
		input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchString(searchString);
		input.setSearchScope(new PluginSearchScope());
		try {
			SearchUI.activateSearchResultView();
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			PluginSearchUIOperation op =
				new PluginSearchUIOperation(
					input,
					new PluginSearchResultCollector());
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1) {
				IFile file =
					(IFile) ((IStructuredSelection) selection)
						.getFirstElement();
				ModelEntry entry =
					PDECore.getDefault().getModelManager().findEntry(
						file.getProject());
				if (entry != null) {
					IPluginModelBase model = entry.getActiveModel();
					if (model != null)
						searchString = model.getPluginBase().getId();
				} else {
					searchString = null;
				}
			}
		}
	}

}
