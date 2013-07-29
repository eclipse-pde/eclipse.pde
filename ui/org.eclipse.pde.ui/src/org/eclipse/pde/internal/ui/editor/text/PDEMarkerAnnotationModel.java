/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

public class PDEMarkerAnnotationModel extends ResourceMarkerAnnotationModel {

	class PDEMarkerAnnotation extends MarkerAnnotation {
		boolean quickFixableState;
		boolean isQuickFixable;

		public PDEMarkerAnnotation(IMarker marker) {
			super(marker);
		}

		@Override
		public void setQuickFixable(boolean state) {
			isQuickFixable = state;
			quickFixableState = true;
		}

		@Override
		public boolean isQuickFixableStateSet() {
			return quickFixableState;
		}

		@Override
		public boolean isQuickFixable() {
			return isQuickFixable;
		}

	}

	public PDEMarkerAnnotationModel(IResource resource) {
		super(resource);
	}

	@Override
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		return new PDEMarkerAnnotation(marker);
	}
}
