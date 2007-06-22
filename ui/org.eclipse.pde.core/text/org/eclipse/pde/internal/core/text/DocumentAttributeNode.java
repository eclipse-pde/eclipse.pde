/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;

/**
 * DocumentAttributeNode
 *
 */
public class DocumentAttributeNode implements IDocumentAttribute {

	private static final long serialVersionUID = 1L;

	private transient IDocumentNode fEnclosingElement;
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
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getAttributeName()
	 */
	public String getAttributeName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getAttributeValue()
	 */
	public String getAttributeValue() {
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getEnclosingElement()
	 */
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getNameLength()
	 */
	public int getNameLength() {
		return fNameLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getNameOffset()
	 */
	public int getNameOffset() {
		return fNameOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getValueLength()
	 */
	public int getValueLength() {
		return fValueLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#getValueOffset()
	 */
	public int getValueOffset() {
		return fValueOffset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setAttributeName(java.lang.String)
	 */
	public void setAttributeName(String name) throws CoreException {
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setAttributeValue(java.lang.String)
	 */
	public void setAttributeValue(String value) throws CoreException {
		fValue = value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setEnclosingElement(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setNameLength(int)
	 */
	public void setNameLength(int length) {
		fNameLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setNameOffset(int)
	 */
	public void setNameOffset(int offset) {
		fNameOffset = offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setValueLength(int)
	 */
	public void setValueLength(int length) {
		fValueLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#setValueOffset(int)
	 */
	public void setValueOffset(int offset) {
		fValueOffset = offset;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#write()
	 */
	public String write() {
		return fName + 
				"=\"" +  //$NON-NLS-1$
				PDEXMLHelper.getWritableAttributeString(fValue) + 
				"\"";  //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#reconnect(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void reconnect(IDocumentNode parent) {
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

}
