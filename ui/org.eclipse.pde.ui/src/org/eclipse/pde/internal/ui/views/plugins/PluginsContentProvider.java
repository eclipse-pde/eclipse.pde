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
 *     Les Jones <lesojones@gmail.com> - bug 191365
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
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
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;

public class PluginsContentProvider implements ITreeContentProvider {

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

	@Override
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

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IDeferredWorkbenchAdapter) {
			if (PDECore.getDefault().getModelManager().isInitialized())
				return PDECore.getDefault().getModelManager().getAllModels();
			Object[] children = fManager.getChildren(parentElement);
			return children;
		}
		if (parentElement instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) parentElement;
			File file = new File(model.getInstallLocation());
			if (!file.isFile()) {
				FileAdapter adapter = new ModelFileAdapter(model, file, PDECore.getDefault().getSearchablePluginsManager());
				return adapter.getChildren();
			}
		}

		if (parentElement instanceof FileAdapter) {
			return ((FileAdapter) parentElement).getChildren();
		}

		if (parentElement instanceof IPackageFragmentRoot || parentElement instanceof IPackageFragment || parentElement instanceof ICompilationUnit || parentElement instanceof IStorage)
			return fJavaProvider.getChildren(parentElement);

		return new Object[0];
	}

	@Override
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

	@Override
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
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	protected IJobChangeListener getCompletionJobListener() {
		return new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK())
					fView.updateContentDescription();
			}

		};
	}

}
