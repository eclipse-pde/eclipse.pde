/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

import java.io.Serializable;

import org.eclipse.core.runtime.CoreException;

public interface IDocumentAttributeNode extends IDocumentRange, Serializable, IDocumentXMLNode {

	// Used by text edit operations

	void setEnclosingElement(IDocumentElementNode node);

	IDocumentElementNode getEnclosingElement();

	void setNameOffset(int offset);

	int getNameOffset();

	void setNameLength(int length);

	int getNameLength();

	void setValueOffset(int offset);

	int getValueOffset();

	void setValueLength(int length);

	int getValueLength();

	String getAttributeName();

	String getAttributeValue();

	void setAttributeName(String name) throws CoreException;

	void setAttributeValue(String value) throws CoreException;

	String write();

	// Not used by text edit operations
	public void reconnect(IDocumentElementNode parent);

}
