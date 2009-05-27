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

/**
 * IDocumentFactory
 *
 */
public interface IDocumentNodeFactory {

	public IDocumentAttributeNode createAttribute(String name, String value, IDocumentElementNode enclosingElement);

	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent);

	public IDocumentTextNode createDocumentTextNode(String content, IDocumentElementNode parent);

}
