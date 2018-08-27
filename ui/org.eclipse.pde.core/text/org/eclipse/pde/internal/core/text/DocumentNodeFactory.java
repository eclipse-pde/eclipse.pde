/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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

	@Override
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

	@Override
	public IDocumentTextNode createDocumentTextNode(String content, IDocumentElementNode parent) {
		IDocumentTextNode textNode = new DocumentTextNode();
		textNode.setEnclosingElement(parent);
		parent.addTextNode(textNode);
		textNode.setText(content);
		return textNode;
	}

	@Override
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
