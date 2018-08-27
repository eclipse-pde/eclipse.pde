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

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

public class TargedDefinitionSetupParticipant implements IDocumentSetupParticipant {

	private TargetDefinitionBufferListener bufferListener;

	public TargedDefinitionSetupParticipant() {
		bufferListener = new TargetDefinitionBufferListener();
		ITextFileBufferManager.DEFAULT.addFileBufferListener(bufferListener);
	}

	@Override
	public void setup(IDocument document) {
		IDocumentListener syntaxListener = new SyntaxValidatorListener();
		bufferListener.setDocument(document);
		bufferListener.setListener(syntaxListener);
		document.addDocumentListener(syntaxListener);
	}
}
