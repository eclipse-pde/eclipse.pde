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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;

/**
 * Marker resolution for adding an api filter to the parent of the member the marker is on.
 * 
 * @since 1.0.0
 */
public class ParentFilterProblemResolution extends FilterProblemResolution {

	/**
	 * Constructor
	 * @param marker
	 */
	public ParentFilterProblemResolution(IMarker marker) {
		super(marker);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.markers.FilterProblemResolution#getImage()
	 */
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPCONT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.markers.FilterProblemResolution#resolveElementFromMarker()
	 */
	protected IJavaElement resolveElementFromMarker() {
		if(fResolvedElement == null) {
			IJavaElement element = JavaCore.create(fBackingMarker.getResource());
			if(element != null) {
				while(element != null) {
					fResolvedElement = element;
					if(element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						break;
					}
					element = element.getParent();
				}
			}
		}
		return fResolvedElement;
	}
}
