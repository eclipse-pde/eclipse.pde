/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.io.File;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.pde.internal.core.ModelFileAdapter;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class PluginsContentProvider extends DefaultContentProvider
				implements ITreeContentProvider, IStructuredContentProvider {
	
	private PluginsView fView;
	private StandardJavaElementContentProvider fJavaProvider;

	/**
	 * Constructor for PluginsContentProvider.
	 */
	public PluginsContentProvider(PluginsView view) {
		fView = view;
		fJavaProvider = new StandardJavaElementContentProvider();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) return;
		fView.updateTitle(newInput);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof PluginModelManager) {
			return ((PluginModelManager) parentElement).getAllModels();
		}
		if (parentElement instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)parentElement;
			if (model != null) {
				File file = new File(model.getInstallLocation());
				if (!file.isFile()) {
					FileAdapter adapter =
						new ModelFileAdapter(
							model,
							file,
							PDECore.getDefault().getSearchablePluginsManager());
					return adapter.getChildren();
				}
			}
		}
			
		if (parentElement instanceof FileAdapter) {
			return ((FileAdapter) parentElement).getChildren();
		}
		
		if (parentElement instanceof IPackageFragmentRoot ||
			parentElement instanceof IPackageFragment ||
			parentElement instanceof ICompilationUnit ||
			parentElement instanceof IStorage) 
			return fJavaProvider.getChildren(parentElement);
		
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof PluginModelManager) {
			return null;
		}
		if (element instanceof IPluginModelBase) {
			return PDECore.getDefault().getModelManager();
		}
		if (element instanceof ModelFileAdapter) {
			return ((ModelFileAdapter) element).getModel();
		}
		if (element instanceof FileAdapter) {
			return ((FileAdapter) element).getParent();
		}
		if (element instanceof IJarEntryResource) {
			return ((IJarEntryResource) element).getParent();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof PluginModelManager) {
			return !((PluginModelManager) element).isEmpty();
		}
		if (element instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)element;
			return model.getUnderlyingResource() == null 
					&& !new File(model.getInstallLocation()).isFile();
		}
		if (element instanceof FileAdapter) {
			FileAdapter fileAdapter = (FileAdapter) element;
			return fileAdapter.hasChildren();
		}
		if (element instanceof IPackageFragmentRoot ||
			element instanceof IPackageFragment ||
			element instanceof ICompilationUnit ||
			element instanceof IStorage)
			return fJavaProvider.hasChildren(element);
		return false;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

}
