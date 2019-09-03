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
package org.eclipse.pde.internal.ui.views.features.support;

import java.util.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureIndex implements IFeatureModelListener {

	private final Map<String, Collection<IFeatureModel>> fIncludingFeatures = new HashMap<>();

	private final FeatureModelManager fFeatureModelManager;

	public FeatureIndex(FeatureModelManager featureModelManager) {
		fFeatureModelManager = featureModelManager;
		fFeatureModelManager.addFeatureModelListener(this);
		reIndex();
	}

	public Collection<IFeatureModel> getIncludingFeatures(String childId) {
		return fIncludingFeatures.getOrDefault(childId, Collections.emptySet());
	}

	public void dispose() {
		fFeatureModelManager.removeFeatureModelListener(this);
	}

	private void reIndex() {
		fIncludingFeatures.clear();
		for (IFeatureModel parentModel : fFeatureModelManager.getWorkspaceModels()) {
			for (IFeatureChild child : parentModel.getFeature().getIncludedFeatures()) {
				IFeatureModel childModel = fFeatureModelManager.findFeatureModel(child.getId());
				if (childModel != null) {
					index(childModel, parentModel);
				}
			}
		}
	}

	private void index(IFeatureModel childModel, IFeatureModel parentModel) {
		String childId = childModel.getFeature().getId();

		Collection<IFeatureModel> parents = fIncludingFeatures.computeIfAbsent(childId, key -> new HashSet<>());
		parents.add(parentModel);
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		reIndex();
	}

}
