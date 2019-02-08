/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class UpdateClasspathResolution extends AbstractPDEMarkerResolution {

	public UpdateClasspathResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.UpdateClasspathResolution_label;
	}

	@Override
	public void run(IMarker marker) {
		IProject project = marker.getResource().getProject();
		IPluginModelBase model = PluginRegistry.findModel(project);
		try {
			ClasspathComputer.setClasspath(project, model);
		} catch (CoreException e) {
		}
	}

	@Override
	protected void createChange(IBaseModel model) {
		// handled by run
	}

}
