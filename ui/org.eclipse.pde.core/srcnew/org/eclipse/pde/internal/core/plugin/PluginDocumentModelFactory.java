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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PluginDocumentModelFactory implements IDocumentModelFactory {

	public IDocumentNode createXMLNode(IDocumentNode[] children, Node domNode) {
		IDocumentNode result= null;
		String parentNodeName = (domNode != null && domNode.getParentNode() != null) ? domNode.getParentNode().getNodeName().toLowerCase() : null;
		if ((hasDepthSmallerOrEqualTo(domNode, 2) || (hasDepthSmallerOrEqualTo(domNode, 3) && ("requires".equals(parentNodeName) || "runtime".equals(parentNodeName)))) && domNode.getNodeType() != Node.TEXT_NODE && domNode.getNodeType() != Node.COMMENT_NODE) {
			result= new PluginDocumentNode(children, domNode);
			result= new PluginDocumentNode(children, domNode);
		}
		return result;
	}

	private boolean hasDepthSmallerOrEqualTo(Node domNode, int level) {
		while (domNode != null && !(domNode instanceof Document) && level > 0) {
			domNode= domNode.getParentNode();
			level--;
		}
		return domNode instanceof Document;
	}
}
