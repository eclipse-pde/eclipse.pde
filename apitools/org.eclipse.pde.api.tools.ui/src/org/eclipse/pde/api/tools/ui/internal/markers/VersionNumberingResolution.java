/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.progress.UIJob;

/**
 * This resolution helps users to pick a default API profile when the tooling has been set up
 * but there is no default profile
 * 
 * @since 1.0.0
 */
public class VersionNumberingResolution implements IMarkerResolution2 {
	String newVersionValue;
	// major or minor version
	private int kind;
	
	public VersionNumberingResolution(IMarker marker) {
		this.newVersionValue = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
		this.kind = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, 0);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		if (IApiProblem.MAJOR_VERSION_CHANGE == this.kind) {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_major0, this.newVersionValue);
		} else if (IApiProblem.MINOR_VERSION_CHANGE == this.kind) {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_minor0, this.newVersionValue);
		} else {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_micro0, this.newVersionValue);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		if (IApiProblem.MAJOR_VERSION_CHANGE == this.kind) {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_major1, this.newVersionValue);
		} else if (IApiProblem.MINOR_VERSION_CHANGE == this.kind) {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_minor1, this.newVersionValue);
		} else {
			return NLS.bind(MarkerMessages.VersionNumberingResolution_micro1, this.newVersionValue);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(final IMarker marker) {
		String title = null;
		if (IApiProblem.MAJOR_VERSION_CHANGE == this.kind) {
			title = NLS.bind(MarkerMessages.VersionNumberingResolution_major2, this.newVersionValue);
		} else if (IApiProblem.MINOR_VERSION_CHANGE == this.kind) {
			title = NLS.bind(MarkerMessages.VersionNumberingResolution_minor2, this.newVersionValue);
		} else {
			title = NLS.bind(MarkerMessages.VersionNumberingResolution_micro2, this.newVersionValue);
		}
		UIJob job  = new UIJob(title) {
			/* (non-Javadoc)
			 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public IStatus runInUIThread(IProgressMonitor monitor) {
				UpdateBundleVersionOperation updateBundleVersionOperation = 
					new UpdateBundleVersionOperation(
							marker,
							VersionNumberingResolution.this.newVersionValue);
				return updateBundleVersionOperation.run(monitor);
			}
		};
		job.setSystem(true);
		job.schedule();
	}
}
