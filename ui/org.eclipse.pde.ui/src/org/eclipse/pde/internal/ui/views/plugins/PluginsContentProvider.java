/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 191365
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.io.File;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;

public class PluginsContentProvider extends DefaultContentProvider implements ITreeContentProvider, IStructuredContentProvider {

	private PluginsView fView;
	private StandardJavaElementContentProvider fJavaProvider;
	private DeferredTreeContentManager fManager = null;

	/**
	 * Constructor for PluginsContentProvider.
	 */
	public PluginsContentProvider(PluginsView view) {
		fView = view;
		fJavaProvider = new StandardJavaElementContentProvider();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) {
			if (fManager != null)
				fManager.cancel(oldInput);
			return;
		}
		fManager = new DeferredTreeContentManager((AbstractTreeViewer) viewer);
		fManager.addUpdateCompleteListener(getCompletionJobListener());
		fView.updateTitle(newInput);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IDeferredWorkbenchAdapter) {
			if (PDECore.getDefault().getModelManager().isInitialized())
				return PDECore.getDefault().getModelManager().getAllModels();
			Object[] children = fManager.getChildren(parentElement);
			return children;
		}
		if (parentElement instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) parentElement;
			if (model != null) {
				File file = new File(model.getInstallLocation());
				if (!file.isFile()) {
					FileAdapter adapter = new ModelFileAdapter(model, file, PDECore.getDefault().getSearchablePluginsManager());
					return adapter.getChildren();
				}
			}
		}

		if (parentElement instanceof FileAdapter) {
			return ((FileAdapter) parentElement).getChildren();
		}

		if (parentElement instanceof IPackageFragmentRoot || parentElement instanceof IPackageFragment || parentElement instanceof ICompilationUnit || parentElement instanceof IStorage)
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
		if (element instanceof IDeferredWorkbenchAdapter)
			return fManager.mayHaveChildren(element);
		if (element instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) element;
			return model.getUnderlyingResource() == null && !new File(model.getInstallLocation()).isFile();
		}
		if (element instanceof FileAdapter) {
			FileAdapter fileAdapter = (FileAdapter) element;
			return fileAdapter.hasChildren();
		}
		if (element instanceof IPackageFragmentRoot || element instanceof IPackageFragment || element instanceof ICompilationUnit || element instanceof IStorage)
			return fJavaProvider.hasChildren(element);
		return false;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	protected IJobChangeListener getCompletionJobListener() {
		return new JobChangeAdapter() {

			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK())
					fView.updateContentDescription();
			}

		};
	}

}
