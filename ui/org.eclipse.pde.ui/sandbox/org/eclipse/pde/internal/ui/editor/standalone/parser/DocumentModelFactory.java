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

package org.eclipse.pde.internal.ui.editor.standalone.parser;

import java.util.*;

import org.w3c.dom.*;

public class DocumentModelFactory implements IDocumentModelFactory {

	public IDocumentNode createXMLNode(IDocumentNode[] children, Node domNode, Hashtable lines) {
		IDocumentNode result = null;
		if (domNode.getNodeType() != Node.TEXT_NODE
			&& domNode.getNodeType() != Node.COMMENT_NODE) {
			result = new DocumentNode(children, domNode, lines);
		}
		return result;
	}

}
