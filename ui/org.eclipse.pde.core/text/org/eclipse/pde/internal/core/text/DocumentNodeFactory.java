/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.text.plugin.DocumentGenericNode;

/**
 * DocumentNodeFactory
 *
 */
public abstract class DocumentNodeFactory implements IDocumentNodeFactory {

	/**
	 * 
	 */
	public DocumentNodeFactory() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentAttributeNode createAttribute(String name, String value, IDocumentElementNode enclosingElement) {

		IDocumentAttributeNode attribute = new DocumentAttributeNode();
		try {
			attribute.setAttributeName(name);
			attribute.setAttributeValue(value);
		} catch (CoreException e) {
			// Ignore
		}
		attribute.setEnclosingElement(enclosingElement);
		return attribute;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentTextNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentTextNode createDocumentTextNode(String content, IDocumentElementNode parent) {
		IDocumentTextNode textNode = new DocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content);
		return textNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent) {
		// Cannot return null
		return createGeneric(name);
	}

	/**
	 * @param name
	 */
	protected IDocumentElementNode createGeneric(String name) {
		return new DocumentGenericNode(name);
	}

}
