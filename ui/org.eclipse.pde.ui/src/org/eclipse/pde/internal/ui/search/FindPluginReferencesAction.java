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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class FindPluginReferencesAction implements IObjectActionDelegate {
	private String fSearchString = null;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		if (fSearchString != null) {
			NewSearchUI.activateSearchResultView();
			NewSearchUI.runQueryInBackground(createSearchQuery());
		}
	}

	private ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchString(fSearchString);
		input.setSearchScope(new PluginSearchScope());
		return new PluginSearchQuery(input);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSearchString = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			if (sSelection.size() == 1) {
				IFile file = (IFile) sSelection.getFirstElement();
				IPluginModelBase model = PluginRegistry.findModel(file.getProject());
				if (model != null)
					fSearchString = model.getPluginBase().getId();
			}
		}
	}

}
