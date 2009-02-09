/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SourcePluginFilter extends ViewerFilter {

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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IPluginModelBase) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			boolean showSourceBundles = store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES);
			if (fState != null && !showSourceBundles) {
				BundleDescription description = ((IPluginModelBase) element).getBundleDescription();
				if (description != null) {
					return fState.getBundleSourceEntry(description.getBundleId()) == null;
				}
			}
		}
		return true;
	}

}
