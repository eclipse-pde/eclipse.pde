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

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class PluginsContentProvider
	extends DefaultContentProvider
	implements ITreeContentProvider, IStructuredContentProvider, IPluginModelListener {
	private PluginModelManager manager;
	private TreeViewer viewer;
	private PluginsView view;
	private StandardJavaElementContentProvider javaProvider;

	/**
	 * Constructor for PluginsContentProvider.
	 */
	public PluginsContentProvider(PluginsView view, PluginModelManager manager) {
		this.manager = manager;
		manager.addPluginModelListener(this);
		this.view = view;
		javaProvider = new StandardJavaElementContentProvider();
	}

	public void dispose() {
		manager.removePluginModelListener(this);
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;
		if (newInput==null) return;
		view.updateTitle(newInput);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof PluginModelManager) {
			return ((PluginModelManager) parentElement).getEntries();
		}
		if (parentElement instanceof ModelEntry) {
			ModelEntry entry = (ModelEntry) parentElement;
			return entry.getChildren();
		}
		if (parentElement instanceof FileAdapter) {
			return ((FileAdapter) parentElement).getChildren();
		}
		if (parentElement instanceof IPackageFragmentRoot ||
			parentElement instanceof IPackageFragment ||
			parentElement instanceof ICompilationUnit) 
			return javaProvider.getChildren(parentElement);
		return new Object[0];
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof PluginModelManager) {
			return null;
		}
		if (element instanceof ModelEntry) {
			return manager;
		}
		if (element instanceof EntryFileAdapter) {
			return ((EntryFileAdapter) element).getEntry();
		}
		if (element instanceof FileAdapter) {
			return ((FileAdapter) element).getParent();
		}
		return null;
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof PluginModelManager) {
			return ((PluginModelManager) element).isEmpty() == false;
		}
		if (element instanceof ModelEntry) {
			ModelEntry entry = (ModelEntry) element;
			return entry.getWorkspaceModel() == null;
		}
		if (element instanceof FileAdapter) {
			FileAdapter fileAdapter = (FileAdapter) element;
			return fileAdapter.hasChildren();
		}
		if (element instanceof IPackageFragmentRoot ||
			element instanceof IPackageFragment ||
			element instanceof ICompilationUnit)
			return javaProvider.hasChildren(element);
		return false;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void modelsChanged(final PluginModelDelta delta) {
		if (viewer == null || viewer.getTree().isDisposed())
			return;

		viewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (viewer.getTree().isDisposed())
					return;
				if ((kind & PluginModelDelta.CHANGED) !=0) {
					// Don't know exactly what change - 
					// the safest way out is to refresh
					viewer.refresh();
					return;
				}
				if ((kind & PluginModelDelta.REMOVED) != 0) {
					ModelEntry[] removed = delta.getRemovedEntries();
					viewer.remove(removed);
				}
				if ((kind & PluginModelDelta.ADDED) != 0) {
					ModelEntry[] added = delta.getAddedEntries();
					for (int i = 0; i < added.length; i++) {
						if (isVisible(added[i]))
							viewer.add(manager, added[i]);
					}
				}
			}
		});
	}
	private boolean isVisible(ModelEntry entry) {
		ViewerFilter[] filters = viewer.getFilters();
		for (int i = 0; i < filters.length; i++) {
			ViewerFilter filter = filters[i];
			if (!filter.select(viewer, manager, entry))
				return false;
		}
		return true;
	}
}