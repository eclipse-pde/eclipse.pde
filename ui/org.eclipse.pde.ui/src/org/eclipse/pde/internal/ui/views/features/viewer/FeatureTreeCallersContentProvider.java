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

import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.views.features.support.FeatureIndex;
import org.eclipse.pde.internal.ui.views.features.support.FeatureInput;

public class FeatureTreeCallersContentProvider extends AbstractFeatureTreeContentProvider {

	private final FeatureIndex fFeatureIndex;

	public FeatureTreeCallersContentProvider(FeatureModelManager featureModelManager,
			FeatureIndex featureIndex) {
		super(featureModelManager);
		fFeatureIndex = featureIndex;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFeatureModel) {
			IFeatureModel featureModel = (IFeatureModel) parentElement;
			String featureId = featureModel.getFeature().getId();
			return fFeatureIndex.getIncludingFeatures(featureId).toArray();
		}

		return new Object[0];
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof FeatureInput) {
			FeatureInput input = (FeatureInput) inputElement;
			FeatureModelManager featureModelManager = input.getFeatureModelManager();
			return featureModelManager.getWorkspaceModels();
		}

		return new Object[0];
	}

}
