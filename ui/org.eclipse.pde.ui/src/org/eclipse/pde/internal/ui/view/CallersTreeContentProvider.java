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
package org.eclipse.pde.internal.ui.view;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class CallersTreeContentProvider extends DefaultContentProvider
		implements ITreeContentProvider, IStructuredContentProvider,
		IPluginModelListener {
	private PluginModelManager fPluginManager;

	private DependenciesView view;

	private StructuredViewer viewer;

	/**
	 * Constructor.
	 */
	public CallersTreeContentProvider(DependenciesView view) {
		this.view = view;
		fPluginManager = PDECore.getDefault().getModelManager();
		fPluginManager.addPluginModelListener(this);
	}

	public void dispose() {
		fPluginManager.removePluginModelListener(this);
	}

	/**
	 * @param id
	 * @return Set of IPluginBase
	 */
	private Set findReferences(String id) {
		ModelEntry[] entries = fPluginManager.getEntries();
		Set l = new HashSet(entries.length);
		for (int i = 0; i < entries.length; i++) {
			IPluginModelBase candidate = entries[i].getActiveModel();
			IPluginBase candidateBase = candidate.getPluginBase(false);
			if (candidateBase == null) {
				continue;
			}
			// refs by require
			IPluginImport[] imports = candidateBase.getImports();
			for (int m = 0; m < imports.length; m++) {
				String candidateId = imports[m].getId();
				if (id.equals(candidateId)) {
					l.add(candidateBase);
				}
			}
			// ref of plugin by fragment
			if (candidateBase instanceof IFragment) {
				String candidateId = ((IFragment) candidateBase).getPluginId();
				if (id.equals(candidateId)) {
					l.add(candidateBase);
				}
			}
		}
		return l;
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object) return Object[] of
	 *      IPluginBase
	 */
	public Object[] getChildren(Object parentElement) {
		String id = null;
		if (parentElement instanceof IPluginModelBase) {
			IPluginBase pluginBase = ((IPluginModelBase) parentElement)
					.getPluginBase(false);
			if (pluginBase != null)
				id = pluginBase.getId();
		} else if (parentElement instanceof IPlugin) {
			id = ((IPlugin) parentElement).getId();
		} else if (parentElement instanceof IPluginImport) {
			id = ((IPluginImport) parentElement).getId();
		}
		if (id == null) {
			return new Object[0];
		}
		Set l = findReferences(id);
		return l.toArray();
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 * @return Object[] with 0 or 1 IPluginBase
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			return new Object[] { ((IPluginModelBase) inputElement)
					.getPluginBase() };
		}
		return new Object[0];
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		return null;
	}

	private void handleRemoved(ModelEntry[] removed) {
		for (int i = 0; i < removed.length; i++) {
			ModelEntry entry = removed[i];
			IPluginModelBase model = entry.getActiveModel();
			if (model != null && model.equals(viewer.getInput())) {
				viewer.setInput(null);
				return;
			}
		}
		viewer.refresh();
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
		if (newInput == null)
			return;
		view.updateTitle(newInput);
	}

	public void modelsChanged(final PluginModelDelta delta) {
		if (viewer == null || viewer.getControl().isDisposed())
			return;

		viewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (viewer.getControl().isDisposed())
					return;
				if ((kind & PluginModelDelta.CHANGED) != 0
						|| (kind & PluginModelDelta.ADDED) != 0) {
					// Don't know exactly what change -
					// the safest way out is to refresh
					viewer.refresh();
					return;
				}
				if ((kind & PluginModelDelta.REMOVED) != 0) {
					ModelEntry[] removed = delta.getRemovedEntries();
					handleRemoved(removed);
				}
				if ((kind & PluginModelDelta.ADDED) != 0) {
					viewer.refresh();
				}
			}
		});
	}
}
