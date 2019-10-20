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
import org.eclipse.pde.internal.ui.views.features.FeaturesView;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;

public abstract class ContentProviderAction extends Action {

	private final FeaturesView fFeaturesView;

	protected final FeaturesViewInput fFeaturesViewInput;

	public ContentProviderAction(FeaturesView featuresView, FeaturesViewInput featuresViewInput) {
		super(null, AS_RADIO_BUTTON);
		fFeaturesView = featuresView;
		fFeaturesViewInput = featuresViewInput;
	}

	public abstract IContentProvider createContentProvider();

	@Override
	public void run() {
		fFeaturesView.setContentProvider(this);
	}

	public abstract boolean isSupportsFeatureChildFilter();

	public abstract boolean isSupportsPlugins();

	public abstract boolean isSupportsProducts();

}