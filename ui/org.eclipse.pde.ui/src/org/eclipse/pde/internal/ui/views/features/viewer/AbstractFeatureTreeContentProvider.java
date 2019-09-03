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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.views.features.support.FeatureInput;

public abstract class AbstractFeatureTreeContentProvider
		implements ITreeContentProvider, IFeatureModelListener {

	protected final FeatureModelManager fFeatureModelManager;

	protected FeatureInput fInput;

	private Viewer fViewer;

	public AbstractFeatureTreeContentProvider(FeatureModelManager featureModelManager) {
		fFeatureModelManager = featureModelManager;
		fFeatureModelManager.addFeatureModelListener(this);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = viewer;

		if (newInput instanceof FeatureInput) {
			fInput = (FeatureInput) newInput;
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
	public void dispose() {
		fFeatureModelManager.removeFeatureModelListener(this);
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		refreshViewer();
	}

	private void refreshViewer() {
		if (fViewer.getControl().isDisposed()) {
			return;
		}

		fViewer.getControl().getDisplay().asyncExec(() -> {
			if (!fViewer.getControl().isDisposed()) {
				fViewer.refresh();
			}
		});
	}

}
