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
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.features.FeaturesView;

public class ShowPluginsAction extends Action {

	private final FeaturesView fFeaturesView;

	public ShowPluginsAction(FeaturesView featuresView) {
		super(null, AS_CHECK_BOX);
		fFeaturesView = featuresView;

		setDescription(PDEUIMessages.FeaturesView_ShowPluginsAction_description);
		setToolTipText(PDEUIMessages.FeaturesView_ShowPluginsAction_tooltip);
		setImageDescriptor(PDEPluginImages.DESC_PLUGINS_FRAGMENTS);
	}

	@Override
	public void run() {
		fFeaturesView.configureContent(featuresViewInput -> featuresViewInput.setIncludePlugins(isChecked()));
	}

}
