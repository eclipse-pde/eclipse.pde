/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.internal.core.*;

public class DependenciesViewPageContentProvider implements IContentProvider, IPluginModelListener {

	private DependenciesView fView;

	private StructuredViewer fViewer;

	/**
	 * Constructor.
	 */
	public DependenciesViewPageContentProvider(DependenciesView view) {
		this.fView = view;
		attachModelListener();
	}

	public void attachModelListener() {
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	public void removeModelListener() {
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
	}

	@Override
	public void dispose() {
		removeModelListener();
	}

	private void handleModifiedModels(ModelEntry[] modified) {
		Object input = fViewer.getInput();
		if (input instanceof IPluginModelBase) {
			BundleDescription desc = ((IPluginModelBase) input).getBundleDescription();
			String inputID = (desc != null) ? desc.getSymbolicName() : ((IPluginModelBase) input).getPluginBase().getId();

			for (ModelEntry entry : modified) {
				if (entry.getId().equals(inputID)) {
					// if we find a matching id to our current input, check to see if the input still exists
					if (modelExists(entry, (IPluginModelBase) input))
						fView.updateTitle(input);
					else
						// if input model does not exist, clear view
						fView.openTo(null);
					return;
				}
			}
		}
	}

	private boolean modelExists(ModelEntry entry, IPluginModelBase input) {
		IPluginModelBase[][] entries = new IPluginModelBase[][] {entry.getExternalModels(), entry.getWorkspaceModels()};
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < entries[i].length; j++) {
				if (entries[i][j].equals(input))
					return true;
			}
		}
		return false;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fView.updateTitle(newInput);
		this.fViewer = (StructuredViewer) viewer;
	}

	@Override
	public void modelsChanged(final PluginModelDelta delta) {
		if (fViewer == null || fViewer.getControl().isDisposed())
			return;

		fViewer.getControl().getDisplay().asyncExec(() -> {
			int kind = delta.getKind();
			if (fViewer.getControl().isDisposed())
				return;
			try {
				if ((kind & PluginModelDelta.REMOVED) != 0) {
					// called when all instances of a Bundle-SymbolicName are all removed
					handleModifiedModels(delta.getRemovedEntries());
				}
				if ((kind & PluginModelDelta.CHANGED) != 0) {
					// called when a plug-in is changed (possibly the input)
					// AND when the model for the ModelEntry changes (new bundle with existing id/remove bundle with 2 instances with same id)
					handleModifiedModels(delta.getChangedEntries());
				}
				if ((kind & PluginModelDelta.ADDED) != 0) {
					// when user modifies Bundle-SymbolicName, a ModelEntry is created for the new name.  In this case, if the input matches
					// the modified model, we need to update the title.
					handleModifiedModels(delta.getAddedEntries());
				}
			} finally {
				// no matter what, refresh the viewer since bundles might un/resolve with changes
				fViewer.refresh();
			}
		});
	}

}
