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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.progress.UIJob;

/**
 * Marker resolution for unsupported API Javadoc tags
 * 
 * @since 1.0.0
 */
public class UnsupportedTagResolution implements IMarkerResolution2 {

	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param marker
	 */
	public UnsupportedTagResolution(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		return getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		try {
			String arg = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = arg.split("#"); //$NON-NLS-1$
			return NLS.bind(MarkerMessages.UnsupportedTagResolution_remove_unsupported_tag, new String[] {args[0]});
		} 
		catch (CoreException e) {}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		UIJob job  = new UIJob(getLabel()) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				RemoveUnsupportedTagOperation op = new RemoveUnsupportedTagOperation(fBackingMarker);
				op.run(monitor);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

}
