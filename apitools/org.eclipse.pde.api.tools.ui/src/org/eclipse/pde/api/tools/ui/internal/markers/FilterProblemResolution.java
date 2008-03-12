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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.ibm.icu.text.MessageFormat;

/**
 * Marker resolution for adding an API filter for the specific member the marker appears on
 * 
 * @since 1.0.0
 */
public class FilterProblemResolution implements IMarkerResolution2 {

	protected IMarker fBackingMarker = null;
	protected IJavaElement fResolvedElement = null;
	
	/**
	 * Constructor
	 * @param marker the backing marker for the resolution
	 */
	public FilterProblemResolution(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		IJavaElement element = resolveElementFromMarker();
		if(element != null) {
			return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {element.getElementName()});
		}
		else {
			IResource res = fBackingMarker.getResource();
			return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {res.getFullPath().removeFileExtension().lastSegment()});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		IJavaElement element = resolveElementFromMarker();
		if(element != null) {
			return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {element.getElementName()});
		}
		else {
			IResource res = fBackingMarker.getResource();
			return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {res.getFullPath().removeFileExtension().lastSegment()});
		}
	}
	
	/**
	 * Resolves the {@link IJavaElement} from the infos in the marker.
	 * 
	 * @return the associated {@link IJavaElement} for the infos in the {@link IMarker}
	 */
	protected IJavaElement resolveElementFromMarker() {
		if(fResolvedElement == null) {
			try {
				String handle = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_HANDLE_ID);
				if(handle != null) {
					fResolvedElement = JavaCore.create(handle);
				}
			}
			catch(CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}
		return fResolvedElement;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		CreateApiFilterOperation op = new CreateApiFilterOperation(fBackingMarker);
		op.setSystem(true);
		op.schedule();
	}
}
