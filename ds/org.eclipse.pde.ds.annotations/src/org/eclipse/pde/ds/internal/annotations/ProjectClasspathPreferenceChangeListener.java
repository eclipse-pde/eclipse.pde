/*******************************************************************************
 * Copyright (c) 2015, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jdt.core.IJavaProject;

public class ProjectClasspathPreferenceChangeListener implements IPreferenceChangeListener, IResourceChangeListener {

	private final IJavaProject project;

	private final ProjectScope scope;

	public ProjectClasspathPreferenceChangeListener(IJavaProject project) {
		this.project = project;
		scope = new ProjectScope(project.getProject());
		scope.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
	}


	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if ((event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE)
				&& project.getProject().equals(event.getResource())) {
			Activator.getDefault().disposeProjectClasspathPreferenceChangeListener(project);
		}
	}

	public void dispose() {
		scope.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
