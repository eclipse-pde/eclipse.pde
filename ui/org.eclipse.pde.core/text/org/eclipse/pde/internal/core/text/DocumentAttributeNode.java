/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import org.eclipse.pde.internal.core.util.PDEXMLHelper;

public class DocumentAttributeNode extends DocumentXMLNode implements IDocumentAttributeNode {

	private static final long serialVersionUID = 1L;

	private transient IDocumentElementNode fEnclosingElement;
	private transient int fNameOffset;
	private transient int fNameLength;
	private transient int fValueOffset;
	private transient int fValueLength;

	private String fValue;
	private String fName;

	/**
	 *
	 */
	public DocumentAttributeNode() {
		fEnclosingElement = null;
		fNameOffset = -1;
		fNameLength = -1;
		fValueOffset = -1;
		fValueLength = -1;
		fValue = null;
		fName = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getAttributeName()
	 */
	@Override
	public String getAttributeName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getAttributeValue()
	 */
	@Override
	public String getAttributeValue() {
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getEnclosingElement()
	 */
	@Override
	public IDocumentElementNode getEnclosingElement() {
		return fEnclosingElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getNameLength()
	 */
	@Override
	public int getNameLength() {
		return fNameLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getNameOffset()
	 */
	@Override
	public int getNameOffset() {
		return fNameOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getValueLength()
	 */
	@Override
	public int getValueLength() {
		return fValueLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#getValueOffset()
	 */
	@Override
	public int getValueOffset() {
		return fValueOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setAttributeName(java.lang.String)
	 */
	@Override
	public void setAttributeName(String name) {
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setAttributeValue(java.lang.String)
	 */
	@Override
	public void setAttributeValue(String value) {
		fValue = value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setEnclosingElement(org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	@Override
	public void setEnclosingElement(IDocumentElementNode node) {
		fEnclosingElement = node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setNameLength(int)
	 */
	@Override
	public void setNameLength(int length) {
		fNameLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setNameOffset(int)
	 */
	@Override
	public void setNameOffset(int offset) {
		fNameOffset = offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setValueLength(int)
	 */
	@Override
	public void setValueLength(int length) {
		fValueLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#setValueOffset(int)
	 */
	@Override
	public void setValueOffset(int offset) {
		fValueOffset = offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#write()
	 */
	@Override
	public String write() {
		return fName + "=\"" + //$NON-NLS-1$
				PDEXMLHelper.getWritableAttributeString(fValue) + "\""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttributeNode#reconnect(org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	@Override
	public void reconnect(IDocumentElementNode parent) {
		// Transient field:  Enclosing element
		// Essentially is the parent (an element)
		// Note: Parent field from plugin document node parent seems to be
		// null; but, we will set it any ways
		fEnclosingElement = parent;
		// Transient field:  Name Length
		fNameLength = -1;
		// Transient field:  Name Offset
		fNameOffset = -1;
		// Transient field:  Value Length
		fValueLength = -1;
		// Transient field:  Value Offset
		fValueOffset = -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentRange#getLength()
	 */
	@Override
	public int getLength() {
		// Implemented for backwards compatibility with utility methods that
		// assume that an attribute is a document range.
		// Stems from the problem that attributes are considered as elements
		// in the hierarchy in the manifest model

		// Includes:  name length + equal + start quote
		int len1 = getValueOffset() - getNameOffset();
		// Includes:  value length
		int len2 = getValueLength();
		// Includes:  end quote
		int len3 = 1;
		// Total
		int length = len1 + len2 + len3;

		return length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentRange#getOffset()
	 */
	@Override
	public int getOffset() {
		// Implemented for backwards compatibility with utility methods that
		// assume that an attribute is a document range.
		// Stems from the problem that attributes are considered as elements
		// in the hierarchy in the manifest model
		return getNameOffset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentXMLNode#getXMLType()
	 */
	@Override
	public int getXMLType() {
		return F_TYPE_ATTRIBUTE;
	}

}
