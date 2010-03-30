/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import java.util.ArrayList;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

/**
 * A wrapper class to fix multiple markers of provided category. It invokes the first non-MultiFixResolution for the selected markers
 *
 */
public class MultiFixResolution extends WorkbenchMarkerResolution {

	IMarker fMarker;
	String fLabel;
	// if quick fix is invoked from editor, then fix all related markers. If invoked from problem view, fix only the selected ones.
	boolean problemViewQuickFix;

	public MultiFixResolution(IMarker marker, String label) {
		fMarker = marker;
		if (label != null)
			fLabel = label;
		else
			fLabel = PDEUIMessages.MultiFixResolution_FixAll;
		problemViewQuickFix = false;
	}

	public IMarker[] findOtherMarkers(IMarker[] markers) {
		ArrayList relatedMarkers = new ArrayList();
		try {
			String markerCategory = (String) fMarker.getAttribute(PDEMarkerFactory.CAT_ID);
			for (int i = 0; i < markers.length; i++) {
				if (markerCategory.equals(markers[i].getAttribute(PDEMarkerFactory.CAT_ID)) && !markers[i].equals(fMarker) && markers[i].getResource().equals(fMarker.getResource())) {
					relatedMarkers.add(markers[i]);
				}
			}
		} catch (CoreException e) {
		}
		problemViewQuickFix = true;
		return (IMarker[]) relatedMarkers.toArray(new IMarker[relatedMarkers.size()]);
	}

	public String getDescription() {
		return getLabel();
	}

	public Image getImage() {
		return PDEPluginImages.DESC_ADD_ATT.createImage();
	}

	public String getLabel() {
		return fLabel;
	}

	public void run(IMarker marker) {
		IResource resource = marker.getResource();
		IMarker[] markers = new IMarker[0];
		try {
			markers = resource.findMarkers(marker.getType(), true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
		}
		if (!problemViewQuickFix) {
			IMarker[] otherMarkers = findOtherMarkers(markers);
			for (int i = 0; i < otherMarkers.length; i++) {
				fixMarker(otherMarkers[i]);
			}
		}
		fixMarker(marker);
	}

	private void fixMarker(IMarker marker) {
		ResolutionGenerator resGen = new ResolutionGenerator();
		IMarkerResolution[] resolutions = resGen.getResolutions(marker);
		for (int i = 0; i < resolutions.length; i++) {
			IMarkerResolution resolution = resolutions[i];
			if (!(resolution instanceof MultiFixResolution)) { // To avoid infinite loop
				resolution.run(marker);
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof MultiFixResolution))
			return false;
		MultiFixResolution multiFix = (MultiFixResolution) obj;
		try {
			String categoryId = (String) multiFix.fMarker.getAttribute(PDEMarkerFactory.CAT_ID);
			if (categoryId == null)
				return false;
			return categoryId.equals(fMarker.getAttribute(PDEMarkerFactory.CAT_ID));
		} catch (CoreException e) {
		}
		return false;
	}

}
