/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

public interface IDocumentNode extends Serializable, IDocumentRange {
		
	IDocumentNode getParentNode();	
	void setParentNode(IDocumentNode node);
	
	void addChildNode(IDocumentNode child);
	void addChildNode(IDocumentNode child, int position);
	IDocumentNode removeChildNode(IDocumentNode child);	
	IDocumentNode[] getChildNodes();
	
	void addTextNode(IDocumentTextNode textNode);
	IDocumentTextNode getTextNode();
	void removeTextNode();
	
	int indexOf(IDocumentNode child);
	IDocumentNode getChildAt(int index);
	
	IDocumentNode getPreviousSibling();
	void setPreviousSibling(IDocumentNode sibling);
	
	void swap(IDocumentNode child1, IDocumentNode child2);
	
	void setXMLTagName(String tag);	
	String getXMLTagName();
	
	void setXMLAttribute(IDocumentAttribute attribute);	
	void setXMLAttribute(String name, String value);	
	String getXMLAttributeValue(String name);
	
	IDocumentAttribute getDocumentAttribute(String name);
	IDocumentAttribute[] getNodeAttributes();
	void removeDocumentAttribute(IDocumentAttribute attr);
	
	boolean isErrorNode();	
	void setIsErrorNode(boolean isErrorNode);
	
	void setOffset(int offset);
	void setLength(int length);
		
	void setLineIndent(int indent);
	int getLineIndent();
	
	String write(boolean indent);
	String writeShallow(boolean terminate);
}
