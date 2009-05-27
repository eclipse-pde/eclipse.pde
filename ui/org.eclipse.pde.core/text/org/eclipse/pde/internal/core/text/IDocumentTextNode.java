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

import java.io.Serializable;

public interface IDocumentTextNode extends IDocumentRange, Serializable, IDocumentXMLNode {

	public static final String F_PROPERTY_CHANGE_TYPE_PCDATA = "type_pcdata"; //$NON-NLS-1$

	// Used by text edit operations
	void setEnclosingElement(IDocumentElementNode node);

	IDocumentElementNode getEnclosingElement();

	void setText(String text);

	String getText();

	void setOffset(int offset);

	void setLength(int length);

	// Not used by text edit operations
	public void reconnect(IDocumentElementNode parent);

	// Not used by text edit operations
	public String write();

}
