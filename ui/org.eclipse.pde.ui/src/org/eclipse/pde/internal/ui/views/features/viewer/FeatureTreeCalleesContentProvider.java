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

import java.util.*;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.views.features.support.FeatureInput;

public class FeatureTreeCalleesContentProvider extends AbstractFeatureTreeContentProvider {

	public FeatureTreeCalleesContentProvider(FeatureModelManager featureModelManager) {
		super(featureModelManager);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFeatureModel) {
			IFeatureModel featureModel = (IFeatureModel) parentElement;
			Object[] features = featureModel.getFeature().getIncludedFeatures();

			if (!fInput.isIncludePlugins()) {
				return features;
			}

			Object[] plugins = featureModel.getFeature().getPlugins();

			List<Object> all = new ArrayList<>(features.length + plugins.length);
			all.addAll(Arrays.asList(features));
			all.addAll(Arrays.asList(plugins));

			return all.toArray();
		} else if (parentElement instanceof IFeatureChild) {
			IFeatureChild featureChild = (IFeatureChild) parentElement;
			IFeatureModel featureModel = fFeatureModelManager.findFeatureModel(featureChild.getId());
			return getChildren(featureModel);
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
