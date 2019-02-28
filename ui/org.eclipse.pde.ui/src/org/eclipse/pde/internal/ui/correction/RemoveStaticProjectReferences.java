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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveStaticProjectReferences extends AbstractPDEMarkerResolution {

	public RemoveStaticProjectReferences(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.RemoveBuildOrderEntries_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveBuildOrderEntries_label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			IProject project = marker.getResource().getProject();
			if (project == null)
				return;
			IProjectDescription projDesc = project.getDescription();
			if (projDesc == null)
				return;
			projDesc.setReferencedProjects(new IProject[0]);
			project.setDescription(projDesc, null);
		} catch (CoreException e) {
		}
	}

	@Override
	protected void createChange(IBaseModel model) {
		// overridden run method handles everything
	}
}
