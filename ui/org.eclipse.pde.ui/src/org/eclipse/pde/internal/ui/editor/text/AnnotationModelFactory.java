/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IAnnotationModelFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.internal.ui.editor.SystemFileMarkerAnnotationModel;

public class AnnotationModelFactory implements IAnnotationModelFactory {

	public IAnnotationModel createAnnotationModel(IPath location) {
		IFile file = FileBuffers.getWorkspaceFileAtLocation(location);
		if (file == null)
			return new SystemFileMarkerAnnotationModel();

		return new PDEMarkerAnnotationModel(file);
	}

}
