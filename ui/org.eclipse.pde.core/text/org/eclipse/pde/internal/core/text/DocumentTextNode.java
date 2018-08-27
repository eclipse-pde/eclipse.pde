/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text;

import java.util.HashMap;
import org.eclipse.pde.internal.core.util.PDETextHelper;

public class DocumentTextNode extends DocumentXMLNode implements IDocumentTextNode {

	private static final long serialVersionUID = 1L;

	protected static final HashMap<Character, String> SUBSTITUTE_CHARS = new HashMap<>(5);

	static {
		SUBSTITUTE_CHARS.put(Character.valueOf('&'), "&amp;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(Character.valueOf('<'), "&lt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(Character.valueOf('>'), "&gt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(Character.valueOf('\''), "&apos;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(Character.valueOf('\"'), "&quot;"); //$NON-NLS-1$
	}

	private transient int fOffset;
	private transient int fLength;
	private transient IDocumentElementNode fEnclosingElement;

	private String fText;

	/**
	 *
	 */
	public DocumentTextNode() {
		fOffset = -1;
		fLength = 0;
		fEnclosingElement = null;
	}

	@Override
	public void setEnclosingElement(IDocumentElementNode node) {
		fEnclosingElement = node;
	}

	@Override
	public IDocumentElementNode getEnclosingElement() {
		return fEnclosingElement;
	}

	@Override
	public void setText(String text) {
		fText = text;
	}

	@Override
	public String getText() {
		return fText == null ? "" : fText; //$NON-NLS-1$
	}

	@Override
	public void setOffset(int offset) {
		fOffset = offset;
	}

	@Override
	public int getOffset() {
		return fOffset;
	}

	@Override
	public int getLength() {
		return fLength;
	}

	@Override
	public void setLength(int length) {
		fLength = length;
	}

	@Override
	public void reconnect(IDocumentElementNode parent) {
		// Transient field:  Enclosing Element
		// Essentially the parent (an element)
		fEnclosingElement = parent;
		// Transient field:  Length
		fLength = -1;
		// Transient field:  Offset
		fOffset = -1;
	}

	@Override
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content, SUBSTITUTE_CHARS);
	}

	@Override
	public int getXMLType() {
		return F_TYPE_TEXT;
	}

}
