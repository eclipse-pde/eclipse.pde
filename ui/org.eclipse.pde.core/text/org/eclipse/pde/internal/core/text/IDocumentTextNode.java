/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.io.Serializable;

public interface IDocumentTextNode extends IDocumentRange, Serializable {
	
	void setEnclosingElement(IDocumentNode node);	
	IDocumentNode getEnclosingElement();

	void setText(String text);
	String getText();
	
	void setOffset(int offset);
	void setLength(int length);
	
	public void reconnect(IDocumentNode parent);
	
}
