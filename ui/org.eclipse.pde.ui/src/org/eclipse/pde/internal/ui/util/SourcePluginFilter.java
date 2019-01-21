/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.util.function.Predicate;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SourcePluginFilter extends ViewerFilter implements Predicate<IPluginModelBase> {

	private PDEState fState;

	public SourcePluginFilter() {
		fState = TargetPlatformHelper.getPDEState();
	}

	public SourcePluginFilter(PDEState state) {
		fState = state;
	}

	public void setState(PDEState state) {
		fState = state;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IPluginModelBase) {
			return test((IPluginModelBase) element);
		}
		return true;
	}

	@Override
	public boolean test(IPluginModelBase element) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean showSourceBundles = store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES);
		if (fState != null && !showSourceBundles) {
			BundleDescription description = element.getBundleDescription();
			if (description != null) {
				return fState.getBundleSourceEntry(description.getBundleId()) == null;
			}
		}

		return true;
	}

}
