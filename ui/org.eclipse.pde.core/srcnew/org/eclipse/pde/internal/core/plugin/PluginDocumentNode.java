/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;


import java.util.*;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.w3c.dom.Node;


/**
 * DefaultXMLDocumentNode.java
 */
public class PluginDocumentNode implements IDocumentNode {

	private IDocumentNode fParentNode;
	private Node fDOMNode;
	private IPluginObject fPluginObjectNode;
	private ArrayList fChildrenList = new ArrayList();
	private boolean fIsErrorNode = false;

	public PluginDocumentNode(Node domNode) {
		fDOMNode = domNode;
	}
	
	public void addChild(IDocumentNode child) {
		fChildrenList.add(child);
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
		IPluginObject object = getPluginObjectNode();
		if (object instanceof IPluginImport || object instanceof IPluginExtension || object instanceof IPluginLibrary)
			return new IDocumentNode[0];
		return (IDocumentNode[])fChildrenList.toArray(new IDocumentNode[fChildrenList.size()]);
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
	
	public void setIsErrorNode(boolean isErrorNode) {
		fIsErrorNode = isErrorNode;
	}
	
	public boolean isErrorNode() {
		return fIsErrorNode;
	}
	
}
