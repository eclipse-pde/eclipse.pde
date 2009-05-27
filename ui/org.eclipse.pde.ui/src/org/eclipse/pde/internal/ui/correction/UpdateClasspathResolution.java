/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	public UpdateClasspathResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return PDEUIMessages.UpdateClasspathResolution_label;
	}

	public void run(IMarker marker) {
		IProject project = marker.getResource().getProject();
		IPluginModelBase model = PluginRegistry.findModel(project);
		try {
			ClasspathComputer.setClasspath(project, model);
		} catch (CoreException e) {
		}
	}

	protected void createChange(IBaseModel model) {
		// handled by run
	}

}
