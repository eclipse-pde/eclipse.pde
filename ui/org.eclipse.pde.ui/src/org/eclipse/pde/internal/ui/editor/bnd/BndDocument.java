/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import aQute.bnd.properties.IRegion;

/**
 * This class wraps from JFace text framework to bnd-document
 */
final class BndDocument implements aQute.bnd.properties.IDocument {

	private final IDocument document;

	public BndDocument(IDocument document) {
		this.document = document;
	}

	@Override
	public int getNumberOfLines() {
		return document.getNumberOfLines();
	}

	@Override
	public IRegion getLineInformation(int lineNum) throws BndBadLocationException {
		try {
			return new BndRegion(document.getLineInformation(lineNum));
		} catch (BadLocationException e) {
			throw new BndBadLocationException(e);
		}
	}

	@Override
	public String get() {
		return document.get();
	}

	@Override
	public String get(int offset, int length) throws BndBadLocationException {
		try {
			return document.get(offset, length);
		} catch (BadLocationException e) {
			throw new BndBadLocationException(e);
		}
	}

	@Override
	public String getLineDelimiter(int line) throws BndBadLocationException {
		try {
			return document.getLineDelimiter(line);
		} catch (BadLocationException e) {
			throw new BndBadLocationException(e);
		}
	}

	@Override
	public int getLength() {
		return document.getLength();
	}

	@Override
	public void replace(int offset, int length, String data) throws BndBadLocationException {
		try {
			document.replace(offset, length, data);
		} catch (BadLocationException e) {
			throw new BndBadLocationException(e);
		}
	}

	@Override
	public char getChar(int offset) throws BndBadLocationException {
		try {
			return document.getChar(offset);
		} catch (BadLocationException e) {
			throw new BndBadLocationException(e);
		}
	}
}
