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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

public class DeferredFeaturesViewInput extends WorkbenchAdapter implements IDeferredWorkbenchAdapter {

	private final FeaturesViewInput fInput;

	public DeferredFeaturesViewInput(FeaturesViewInput input) {
		fInput = input;
	}

	public FeaturesViewInput getFeaturesViewInput() {
		return fInput;
	}

	public boolean isInitialized() {
		return fInput.isInitialized();
	}

	@Override
	public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
		Object[] children = getChildren(object);
		updateTree(collector, monitor, children);
	}

	@Override
	public Object[] getChildren(Object object) {
		return fInput.getModels();
	}

	private void updateTree(IElementCollector collector, IProgressMonitor monitor, Object[] children) {
		collector.add(children, monitor);
		collector.done();
	}

	@Override
	public String getLabel(Object object) {
		return PDEUIMessages.FeaturesView_loadingDescription;
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public ISchedulingRule getRule(Object object) {
		return null;
	}

}
