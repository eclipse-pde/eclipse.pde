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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class CalleesTreeContentProvider extends DefaultContentProvider
		implements ITreeContentProvider, IStructuredContentProvider,
		IPluginModelListener {
	private PluginModelManager fPluginManager;

	private DependenciesView fView;

	private StructuredViewer fViewer;

	/**
	 * Constructor.
	 */
	public CalleesTreeContentProvider(DependenciesView view) {
		this.fView = view;
		fPluginManager = PDECore.getDefault().getModelManager();
		fPluginManager.addPluginModelListener(this);
	}

	private Object[] createImportObjects(IPluginBase plugin) {
		IPluginImport[] imports = plugin.getImports();
		Object[] result = new Object[imports.length];
		for (int i = 0; i < imports.length; i++) {
			result[i] = new ImportObject(imports[i]);
		}
		return result;
	}

	public void dispose() {
		fPluginManager.removePluginModelListener(this);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IPluginBase) {
			return createImportObjects((IPluginBase) parentElement);
		}
		if (parentElement instanceof ImportObject) {
			ImportObject iobj = (ImportObject) parentElement;
			IPlugin plugin = iobj.getPlugin();
			if (plugin == null)
				return new Object[0];
			return createImportObjects(plugin);
		}
		return new Object[0];
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
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
			if (model != null && model.equals(fViewer.getInput())) {
				fViewer.setInput(null);
				return;
			}
		}
		fViewer.refresh();
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.fViewer = (StructuredViewer) viewer;
		if (newInput == null)
			return;
		fView.updateTitle(newInput);
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
}
