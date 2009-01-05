/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.io.InputStream;
import java.net.URI;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

public class PDEStorageDocumentProvider extends StorageDocumentProvider {

	private IDocumentSetupParticipant fSetupParticipant;

	public PDEStorageDocumentProvider(IDocumentSetupParticipant participant) {
		fSetupParticipant = participant;
	}

	// we need to override this method when dealing with IURIEditorInput's...
	// is there a better way to do this?
	protected boolean setDocumentContent(IDocument document, IEditorInput editorInput, String encoding) throws CoreException {
		boolean set = super.setDocumentContent(document, editorInput, encoding);
		if (!set) {
			if (editorInput instanceof IURIEditorInput) {
				IURIEditorInput input = (IURIEditorInput) editorInput;
				IFileStore store = EFS.getStore(input.getURI());
				InputStream is = store.openInputStream(EFS.CACHE, new NullProgressMonitor());
				setDocumentContent(document, is, encoding);
				set = true;
			}
		}
		return set;
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
			IAdaptable input = (IAdaptable) element;
			File file = (File) input.getAdapter(File.class);
			if (file == null && (input instanceof IURIEditorInput)) {
				URI uri = ((IURIEditorInput) input).getURI();
				file = new File(uri);
			}
			if (file != null) {
				return new SystemFileMarkerAnnotationModel();
			}
		}
		return super.createAnnotationModel(element);
	}
}
