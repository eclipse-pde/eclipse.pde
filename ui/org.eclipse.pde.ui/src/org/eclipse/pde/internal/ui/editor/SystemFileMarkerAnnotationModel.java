/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

/**
 * A marker annotation model whose underlying source of markers is
 * a resource in the workspace.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class SystemFileMarkerAnnotationModel extends AbstractMarkerAnnotationModel {

	@Override
	protected IMarker[] retrieveMarkers() throws CoreException {
		return null;
	}

	@Override
	protected void deleteMarkers(IMarker[] markers) throws CoreException {
	}

	@Override
	protected void listenToMarkerChanges(boolean listen) {
	}

	@Override
	protected boolean isAcceptable(IMarker marker) {
		return true;
	}

}
