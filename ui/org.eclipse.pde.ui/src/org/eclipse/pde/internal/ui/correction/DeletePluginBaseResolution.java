/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class DeletePluginBaseResolution extends AbstractPDEMarkerResolution {

	public DeletePluginBaseResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return PDEUIMessages.RemoveUselessPluginFile_description;
	}

	public void run(final IMarker marker) {
		try {
			marker.delete();
			marker.getResource().deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
			marker.getResource().delete(true, new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}

	protected void createChange(IBaseModel model) {
		// handled by run
	}

}