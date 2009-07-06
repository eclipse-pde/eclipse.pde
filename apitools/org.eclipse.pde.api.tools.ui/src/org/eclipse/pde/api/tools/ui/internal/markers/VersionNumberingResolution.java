/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
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
	private String description;
	
	public VersionNumberingResolution(IMarker marker) {
		this.newVersionValue = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
		this.kind = ApiProblemFactory.getProblemKind(marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0));
		this.description = marker.getAttribute(IApiMarkerConstants.VERSION_NUMBERING_ATTR_DESCRIPTION, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		switch(this.kind) {
			case IApiProblem.MAJOR_VERSION_CHANGE :
				return NLS.bind(
						MarkerMessages.VersionNumberingResolution_major0,
						new String[] {this.description});
			case IApiProblem.MINOR_VERSION_CHANGE :
				return NLS.bind(
						MarkerMessages.VersionNumberingResolution_minor0,
						new String[] {this.description});
			case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE :
				return NLS.bind(
						MarkerMessages.VersionNumberingResolution_major0,
						new String[] {this.description});
			case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API :
				return MarkerMessages.VersionNumberingResolution_minorNoNewAPI0;
			case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
				return MarkerMessages.VersionNumberingResolution_reexportedMajor0;
			default :
				// reexported minor
				return MarkerMessages.VersionNumberingResolution_reexportedMinor0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_BUNDLE_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		switch(this.kind) {
			case IApiProblem.MAJOR_VERSION_CHANGE :
				return NLS.bind(MarkerMessages.VersionNumberingResolution_major1, this.newVersionValue);
			case IApiProblem.MINOR_VERSION_CHANGE :
				return NLS.bind(MarkerMessages.VersionNumberingResolution_minor1, this.newVersionValue);
			case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE :
				return NLS.bind(MarkerMessages.VersionNumberingResolution_major1, this.newVersionValue);
			case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API :
				return NLS.bind(MarkerMessages.VersionNumberingResolution_minorNoNewAPI1, this.newVersionValue);
			case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
				return NLS.bind(MarkerMessages.VersionNumberingResolution_reexportedMajor1, this.newVersionValue);
			default:
				// IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE
				return NLS.bind(MarkerMessages.VersionNumberingResolution_reexportedMinor1, this.newVersionValue);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(final IMarker marker) {
		String title = null;
		switch(this.kind) {
			case IApiProblem.MAJOR_VERSION_CHANGE :
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_major2, this.newVersionValue);
				break;
			case IApiProblem.MINOR_VERSION_CHANGE :
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_minor2, this.newVersionValue);
				break;
			case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE :
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_major2, this.newVersionValue);
				break;
			case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API :
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_minorNoNewAPI2, this.newVersionValue);
				break;
			case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_reexportedMajor2, this.newVersionValue);
				break;
			default :
				// IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE
				title = NLS.bind(MarkerMessages.VersionNumberingResolution_reexportedMinor2, this.newVersionValue);
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
