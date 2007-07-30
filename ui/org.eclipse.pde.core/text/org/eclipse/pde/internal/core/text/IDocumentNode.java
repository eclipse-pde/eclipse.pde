/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.pde.core.IModel;

public interface IDocumentNode extends Serializable, IDocumentRange {
		
	IDocumentNode getParentNode();	
	void setParentNode(IDocumentNode node);
	
	void addChildNode(IDocumentNode child);
	void addChildNode(IDocumentNode child, int position);
	IDocumentNode removeChildNode(IDocumentNode child);	
	// Not used by text edit operations
	IDocumentNode removeChildNode(int index); 
	
	IDocumentNode[] getChildNodes();
	
	void addTextNode(IDocumentTextNode textNode);
	IDocumentTextNode getTextNode();
	void removeTextNode();
	
	// Not used by text edit operations
	int indexOf(IDocumentNode child);
	
	IDocumentNode getChildAt(int index);
	
	IDocumentNode getPreviousSibling();
	void setPreviousSibling(IDocumentNode sibling);
	
	// Not used by text edit operations
	void swap(IDocumentNode child1, IDocumentNode child2);
	
	void setXMLTagName(String tag);	
	String getXMLTagName();
	
	void setXMLAttribute(IDocumentAttribute attribute);	
	
	// Not used by text edit operations
	public boolean setXMLAttribute(String name, String value);	
	// Not used by text edit operations
	String getXMLAttributeValue(String name);
	
	IDocumentAttribute getDocumentAttribute(String name);
	IDocumentAttribute[] getNodeAttributes();
	void removeDocumentAttribute(IDocumentAttribute attr);
	
	boolean isErrorNode();	
	void setIsErrorNode(boolean isErrorNode);
	
	boolean isRoot();
	
	void setOffset(int offset);
	void setLength(int length);
		
	void setLineIndent(int indent);
	int getLineIndent();
	// Not used by text edit operations
	public String getIndent();
	
	String write(boolean indent);
	String writeShallow(boolean terminate);
	
	// Not used by text edit operations
	public int getChildCount();
	// Not used by text edit operations
	public TreeMap getNodeAttributesMap();
	// Not used by text edit operations
	public ArrayList getChildNodesList();
	// Not used by text edit operations
	public void reconnect(IDocumentNode parent, IModel model);

	// Not used by text edit operations
	/**
	 * @param text String already trimmed and formatted
	 * @return
	 */
	public boolean setXMLContent(String text);
	// Not used by text edit operations
	public String getXMLContent();
	
	// TODO: MP: TEO:  Rename to IDocumentElementNode
	// TODO: MP: TEO:  Space out, comment and rename methods
	// TODO: MP: TEO: Consider making separate interface for plugins only as subinterface
}
