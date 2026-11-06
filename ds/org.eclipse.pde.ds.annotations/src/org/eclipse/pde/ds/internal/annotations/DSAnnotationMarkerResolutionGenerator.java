/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class DSAnnotationMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	private static final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			if (!"org.eclipse.pde.ds.annotations.problem".equals(marker.getType())) { //$NON-NLS-1$
				return false;
			}
			String message = (String) marker.getAttribute(IMarker.MESSAGE);
			if (!Messages.AnnotationProcessor_invalidLifecycleMethod_noMethod.equals(message)) {
				return false;
			}
			IProject project = marker.getResource().getProject();
			if (project == null || !project.isAccessible()) {
				return false;
			}
			IPreferencesService prefs = Platform.getPreferencesService();
			IScopeContext[] scope = new IScopeContext[] { new ProjectScope(project), InstanceScope.INSTANCE,
					DefaultScope.INSTANCE };
			String specVersionStr = prefs.getString(Activator.PLUGIN_ID, Activator.PREF_SPEC_VERSION,
					DSAnnotationVersion.V1_3.name(), scope);
			DSAnnotationVersion specVersion;
			try {
				specVersion = DSAnnotationVersion.valueOf(specVersionStr);
			} catch (IllegalArgumentException e) {
				specVersion = DSAnnotationVersion.V1_3;
			}
			return specVersion.compareTo(DSAnnotationVersion.V1_3) <= 0;
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (hasResolutions(marker)) {
			return new IMarkerResolution[] { new UpgradeDSAnnotationsResolution() };
		}
		return NO_RESOLUTIONS;
	}
}
