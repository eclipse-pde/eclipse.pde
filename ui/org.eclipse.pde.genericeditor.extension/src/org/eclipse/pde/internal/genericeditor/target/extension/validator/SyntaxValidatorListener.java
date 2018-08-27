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

import java.util.concurrent.CompletableFuture;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;

public class SyntaxValidatorListener implements IDocumentListener {

	private static final String ERROR_MARKER = "org.eclipse.jdt.ui.error";

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {

	}

	@Override
	public void documentChanged(DocumentEvent event) {
		IDocument fDocument = event.fDocument;
		ITextFileBuffer textFileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(event.getDocument());
		if (textFileBuffer == null)
			return;
		IAnnotationModel model = textFileBuffer
				.getAnnotationModel();
		// clear the annotations
		model.getAnnotationIterator().forEachRemaining(model::removeAnnotation);
		CompletableFuture.runAsync(() -> {
			try {
				Parser.getDefault().parse(fDocument);
			} catch (XMLStreamException e) {
				Annotation error = prepareAnnotation(e);
				Position position = preparePosition(e);
				model.addAnnotation(error, position);
			}
		});
	}

	private Position preparePosition(XMLStreamException e) {
		int offset = e.getLocation().getCharacterOffset();
		Position position = new Position(offset);
		return position;
	}

	private Annotation prepareAnnotation(XMLStreamException e) {
		Annotation annotation = new Annotation(ERROR_MARKER, true, beautify(e.getLocalizedMessage()));
		return annotation;
	}

	private String beautify(String localizedMessage) {
		String startOfInterest = "Message:";
		int colonIndex = localizedMessage.indexOf(startOfInterest);
		return localizedMessage.substring(colonIndex + startOfInterest.length());
	}

}
