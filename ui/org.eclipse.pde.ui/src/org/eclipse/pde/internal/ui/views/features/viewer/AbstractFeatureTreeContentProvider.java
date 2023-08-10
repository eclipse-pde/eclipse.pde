/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.viewer;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.IFeatureModelDelta;
import org.eclipse.pde.internal.core.IFeatureModelListener;
import org.eclipse.pde.internal.ui.views.features.model.IProductModelListener;
import org.eclipse.pde.internal.ui.views.features.model.ProductModelManager;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.progress.DeferredTreeContentManager;

public abstract class AbstractFeatureTreeContentProvider
		implements ITreeContentProvider, IFeatureModelListener, IProductModelListener {

	protected final FeatureModelManager fFeatureModelManager;

	protected final ProductModelManager fProductModelManager;

	protected DeferredFeaturesViewInput fInput;

	protected DeferredTreeContentManager fDeferredTreeContentManager;

	private TreeViewer fViewer;

	public AbstractFeatureTreeContentProvider(FeaturesViewInput featuresViewInput) {
		fFeatureModelManager = featuresViewInput.getFeatureSupport().getManager();
		fFeatureModelManager.addFeatureModelListener(this);
		fProductModelManager = featuresViewInput.getProductSupport().getManager();
		fProductModelManager.addProductModelListener(this);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = (TreeViewer) viewer;
		fDeferredTreeContentManager = new DeferredTreeContentManager(fViewer);
		fDeferredTreeContentManager.addUpdateCompleteListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				resetViewerScrollPosition();
			}
		});

		if (newInput instanceof DeferredFeaturesViewInput) {
			fInput = (DeferredFeaturesViewInput) newInput;
		}
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof DeferredFeaturesViewInput deferredFeaturesViewInput) {
			return deferredFeaturesViewInput.isInitialized() ? deferredFeaturesViewInput.getChildren(inputElement)
					: fDeferredTreeContentManager.getChildren(inputElement);
		}

		return new Object[0];
	}

	@Override
	public void dispose() {
		fFeatureModelManager.removeFeatureModelListener(this);
		fProductModelManager.removeProductModelListener(this);
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		refreshViewer();
	}

	@Override
	public void modelsChanged() {
		refreshViewer();
	}

	private void refreshViewer() {
		runViewerTask(fViewer::refresh);
	}

	private void resetViewerScrollPosition() {
		runViewerTask(() -> {
			Tree tree = fViewer.getTree();
			if (tree.getItemCount() > 0) {
				TreeItem firstItem = tree.getItem(0);
				tree.setTopItem(firstItem);
			}
		});
	}

	private void runViewerTask(Runnable viewerTask) {
		if (fViewer.getTree().isDisposed()) {
			return;
		}

		fViewer.getTree().getDisplay().asyncExec(() -> {
			if (!fViewer.getTree().isDisposed()) {
				viewerTask.run();
			}
		});
	}

}
