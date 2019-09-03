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
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.ui.views.features.FeaturesView;

public abstract class ViewerFilterAction extends Action {

	private final FeaturesView fFeaturesView;

	private final ViewerFilter fFilter;

	public ViewerFilterAction(FeaturesView featuresView, ViewerFilter filter) {
		super(null, AS_CHECK_BOX);
		fFeaturesView = featuresView;
		fFilter = filter;
		setChecked(featuresView.isActive(filter));
	}

	@Override
	public void run() {
		fFeaturesView.toggle(fFilter);
	}

	public ViewerFilter getViewerFilter() {
		return fFilter;
	}

}
