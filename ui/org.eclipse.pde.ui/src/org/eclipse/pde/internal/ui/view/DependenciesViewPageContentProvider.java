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

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class DependenciesViewPageContentProvider extends DefaultContentProvider
		implements IPluginModelListener {
	private PluginModelManager fPluginManager;

	private DependenciesView fView;

	private StructuredViewer fViewer;

	/**
	 * Constructor.
	 */
	public DependenciesViewPageContentProvider(DependenciesView view) {
		this.fView = view;
		fPluginManager = PDECore.getDefault().getModelManager();
		fPluginManager.addPluginModelListener(this);
	}

	public void dispose() {
		fPluginManager.removePluginModelListener(this);
	}

	private void handleRemoved(ModelEntry[] removed) {
		for (int i = 0; i < removed.length; i++) {
			ModelEntry entry = removed[i];
			IPluginModelBase model = entry.getActiveModel();
			if (model != null && model.equals(fViewer.getInput())) {
				fViewer.setInput(null);
				return;
			}
		}
		fViewer.refresh();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fView.updateTitle(newInput);
		this.fViewer = (StructuredViewer) viewer;
	}

	public void modelsChanged(final PluginModelDelta delta) {
		if (fViewer == null || fViewer.getControl().isDisposed())
			return;

		fViewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (fViewer.getControl().isDisposed())
					return;
				if ((kind & PluginModelDelta.CHANGED) != 0
						|| (kind & PluginModelDelta.ADDED) != 0) {
					// Don't know exactly what change -
					// the safest way out is to refresh
					fViewer.refresh();
					return;
				}
				if ((kind & PluginModelDelta.REMOVED) != 0) {
					ModelEntry[] removed = delta.getRemovedEntries();
					handleRemoved(removed);
				}
				if ((kind & PluginModelDelta.ADDED) != 0) {
					fViewer.refresh();
				}
			}
		});
	}

	/**
	 * @return Returns the fPluginManager.
	 */
	protected PluginModelManager getPluginManager() {
		return fPluginManager;
	}
}
