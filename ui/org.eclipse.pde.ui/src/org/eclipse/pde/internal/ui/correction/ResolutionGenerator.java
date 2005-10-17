/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.builders.PDEMarkerFactory;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class ResolutionGenerator implements IMarkerResolutionGenerator2 {
	
	private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		int problemID = marker.getAttribute("id", PDEMarkerFactory.NO_RESOLUTION); //$NON-NLS-1$
		switch (problemID) {
			case PDEMarkerFactory.DEPRECATED_AUTOSTART:
				return new IMarkerResolution[] {new RenameAutostartResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case PDEMarkerFactory.JAVA_PACKAGE__PORTED:
				return new IMarkerResolution[] {new CreateJREBundleHeaderResolution(AbstractPDEMarkerResolution.CREATE_TYPE)};
			case PDEMarkerFactory.SINGLETON_DIR_NOT_SET:
				return new IMarkerResolution[] {new AddSingleonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, true)};
			case PDEMarkerFactory.SINGLETON_ATT_NOT_SET:
				return new IMarkerResolution[] {new AddSingleonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, false)};
		}
		return NO_RESOLUTIONS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute("id", PDEMarkerFactory.NO_RESOLUTION) > 0; //$NON-NLS-1$
	}

}
