/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.pde.internal.core.plugin;


import org.eclipse.pde.core.plugin.IPluginObject;
import org.w3c.dom.Node;


/**
 * DefaultXMLDocumentNode.java
 */
public class PluginDocumentNode implements IDocumentNode {

	private IDocumentNode[] fChildren;
	private IDocumentNode fParentNode;
	private Node fDOMNode;
	private IPluginObject fPluginObjectNode;

	public PluginDocumentNode(IDocumentNode[] children, Node domNode) {
		fChildren= children;
		fDOMNode= domNode;
	}

	public String getText() {
		String result= null;
		if (fPluginObjectNode != null) {
			result= fPluginObjectNode.getName();
		} else if (fDOMNode != null) {
			result= fDOMNode.getNodeName(); 
		}
		return result;
	}
	
	public IPluginObject getPluginObjectNode() {
		return fPluginObjectNode;
	}
	
	public void setPluginObjectNode(IPluginObject pluginObject) {
		fPluginObjectNode= pluginObject;
	}
	
	public IDocumentNode[] getChildren() {
		return fChildren;
	}

	public IDocumentNode getParent() {
		return fParentNode;
	}

	public void setParent(IDocumentNode parentNode) {
		fParentNode= parentNode;
	}

	public Node getDOMNode() {
		return fDOMNode;
	}

	private DocumentModel findDocumentModel() {
		IDocumentNode node= fParentNode;
		while (node != null) {
			if (node instanceof DocumentModel)
				return (DocumentModel) node;
			node= node.getParent();
		}
		return null;
	}
	
	public ISourceRange getSourceRange() {
		DocumentModel model= findDocumentModel();
		if (model != null)
			return model.getSourceRange(this);
		return null;
	}
	
}
