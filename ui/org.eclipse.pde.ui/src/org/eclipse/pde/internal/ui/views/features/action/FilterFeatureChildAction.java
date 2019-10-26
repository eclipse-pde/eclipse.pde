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

import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.features.FeaturesView;
import org.eclipse.pde.internal.ui.views.features.viewer.FeatureChildViewerFilter;

public class FilterFeatureChildAction extends ViewerFilterAction {

	public FilterFeatureChildAction(FeaturesView featuresView) {
		super(featuresView, new FeatureChildViewerFilter());

		setDescription(PDEUIMessages.FeaturesView_FilterFeatureChildAction_description);
		setToolTipText(PDEUIMessages.FeaturesView_FilterFeatureChildAction_tooltip);
		setImageDescriptor(PDEPluginImages.DESC_FULL_HIERARCHY);
	}

}
