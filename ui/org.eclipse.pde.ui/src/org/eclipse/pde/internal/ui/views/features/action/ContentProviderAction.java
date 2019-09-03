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
package org.eclipse.pde.internal.ui.views.features.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.ui.views.features.FeaturesView;

public abstract class ContentProviderAction extends Action {

	private final FeaturesView fFeaturesView;

	protected final FeatureModelManager fFeatureModelManager;

	public ContentProviderAction(FeaturesView featuresView, FeatureModelManager featureModelManager) {
		super(null, AS_RADIO_BUTTON);
		fFeaturesView = featuresView;
		fFeatureModelManager = featureModelManager;
	}

	public abstract ViewerComparator createViewerComparator();

	public abstract IContentProvider createContentProvider();

	@Override
	public void run() {
		fFeaturesView.setContentProvider(this);
	}

	public abstract boolean isSupportsFilters();

	public abstract boolean isSupportsPlugins();

}