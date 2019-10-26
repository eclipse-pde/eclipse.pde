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

import java.util.Collection;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureInput {

	private final FeatureModelManager fFeatureModelManager;

	private final FeatureIndex fIndex;

	private boolean fIncludePlugins;

	public FeatureInput(FeatureModelManager featureModelManager) {
		fFeatureModelManager = featureModelManager;
		fIndex = new FeatureIndex(featureModelManager);
	}

	public boolean isInitialized() {
		return fFeatureModelManager.isInitialized() && fIndex.isInitialized();
	}

	public IFeatureModel[] getFeatures() {
		ensureInitialized();
		return fFeatureModelManager.getModels();
	}

	public Collection<IFeatureModel> getIncludingFeatures(String childId) {
		ensureInitialized();
		return fIndex.getIncludingFeatures(childId);
	}

	private void ensureInitialized() {
		fIndex.ensureInitialized();
	}

	public boolean isIncludePlugins() {
		return fIncludePlugins;
	}

	public void setIncludePlugins(boolean includePlugins) {
		fIncludePlugins = includePlugins;
	}

	public void dispose() {
		fIndex.dispose();
	}

}
