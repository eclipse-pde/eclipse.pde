/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.File;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

public class PDEStorageDocumentProvider extends StorageDocumentProvider {
	
	private IDocumentSetupParticipant fSetupParticipant;

	public PDEStorageDocumentProvider(IDocumentSetupParticipant participant) {
		fSetupParticipant = participant;
	}
	
	protected void setupDocument(Object element, IDocument document) {
		if (document != null && fSetupParticipant != null) {
			fSetupParticipant.setup(document);
		}
	}

	/*
	 * @see AbstractDocumentProvider#createAnnotationModel(Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		if (element instanceof IAdaptable) {
			IAdaptable input= (IAdaptable) element;
			File file = (File)input.getAdapter(File.class);
			if (file != null) {
				return new SystemFileMarkerAnnotationModel();
			}
		}
		return super.createAnnotationModel(element);
	}


}
