/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.util.HashMap;

import org.eclipse.pde.internal.core.util.PDETextHelper;

public class DocumentTextNode extends DocumentXMLNode implements IDocumentTextNode {

	private static final long serialVersionUID = 1L;

	protected static final HashMap SUBSTITUTE_CHARS = new HashMap(5);

	static {
		SUBSTITUTE_CHARS.put(new Character('&'), "&amp;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('<'), "&lt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('>'), "&gt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\''), "&apos;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\"'), "&quot;"); //$NON-NLS-1$
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentElementNode node) {
		fEnclosingElement = node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getEnclosingElement()
	 */
	public IDocumentElementNode getEnclosingElement() {
		return fEnclosingElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setText(java.lang.String)
	 */
	public void setText(String text) {
		fText = text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getText()
	 */
	public String getText() {
		return fText == null ? "" : fText; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#getLength()
	 */
	public int getLength() {
		return fLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentTextNode#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentTextNode#reconnectText(org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public void reconnect(IDocumentElementNode parent) {
		// Transient field:  Enclosing Element
		// Essentially the parent (an element)
		fEnclosingElement = parent;
		// Transient field:  Length
		fLength = -1;
		// Transient field:  Offset
		fOffset = -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentTextNode#write()
	 */
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content, SUBSTITUTE_CHARS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentXMLNode#getXMLType()
	 */
	public int getXMLType() {
		return F_TYPE_TEXT;
	}

}
