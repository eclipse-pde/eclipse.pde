/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.validator;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

public class TargetDefinitionBufferListener implements IFileBufferListener {

	private static final String TARGET_DEFINITION_CONTENT_TYPE_ID = "org.eclipse.pde.targetFile";
	private IDocument doc;
	private IDocumentListener listener;

	@Override
	public void underlyingFileMoved(IFileBuffer buffer, IPath path) {

	}

	@Override
	public void underlyingFileDeleted(IFileBuffer buffer) {

	}

	@Override
	public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {

	}

	@Override
	public void stateChanging(IFileBuffer buffer) {

	}

	@Override
	public void stateChangeFailed(IFileBuffer buffer) {

	}

	@Override
	public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {

	}

	@Override
	public void bufferDisposed(IFileBuffer buffer) {

	}

	@Override
	public void bufferCreated(IFileBuffer buffer) {

		if (buffer == null) {
			return;
		}

		IContentType contentType = null;
		try {
			contentType = buffer.getContentType();
		} catch (CoreException e) {
		}

		if (contentType == null) {
			return;
		}

		if (listener == null) {
			return;
		}

		if (TARGET_DEFINITION_CONTENT_TYPE_ID.equals(contentType.getId())) {
			DocumentEvent event = new DocumentEvent();
			event.fDocument = doc;
			listener.documentChanged(event);
		}

	}

	@Override
	public void bufferContentReplaced(IFileBuffer buffer) {

	}

	@Override
	public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {

	}

	public void setDocument(IDocument document) {
		doc = document;
	}

	public void setListener(IDocumentListener listener) {
		this.listener = listener;
	}
}