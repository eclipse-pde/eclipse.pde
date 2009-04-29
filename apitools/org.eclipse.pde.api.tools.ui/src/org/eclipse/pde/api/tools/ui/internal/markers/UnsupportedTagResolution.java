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

import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

/**
 * Marker resolution for unsupported API Javadoc tags
 * 
 * @since 1.0.0
 */
public class UnsupportedTagResolution extends WorkbenchMarkerResolution {

	protected IMarker fBackingMarker = null;
	private boolean plural = false;
	
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
		if(this.plural) {
			return MarkerMessages.UnsupportedTagResolution_remove_unsupported_tags;
		}
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
		if(this.plural) {
			return getDescription();
		}
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
		RemoveUnsupportedTagOperation op = new RemoveUnsupportedTagOperation(new IMarker[] {marker});
		op.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#run(org.eclipse.core.resources.IMarker[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		RemoveUnsupportedTagOperation op = new RemoveUnsupportedTagOperation(markers);
		op.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#findOtherMarkers(org.eclipse.core.resources.IMarker[])
	 */
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet mset = new HashSet(markers.length);
		for (int i = 0; i < markers.length; i++) {
			if(Util.isApiProblemMarker(markers[i]) &&
					!fBackingMarker.equals(markers[i]) &&
					markers[i].getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1) == IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID) {
				mset.add(markers[i]);
			}
		}
		int size = mset.size();
		plural = size > 0;
		return (IMarker[]) mset.toArray(new IMarker[size]);
	}
}
