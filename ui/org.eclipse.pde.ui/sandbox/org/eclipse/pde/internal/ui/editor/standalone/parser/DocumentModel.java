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

import java.io.*;

import org.apache.xerces.xni.parser.*;
import org.w3c.dom.*;

/**
 * PluginXMLDocumentModel.java
 */
public class DocumentModel  implements IDocumentNode {
	
	private XMLDocumentModelBuilder fParser;
	private XEErrorHandler fErrorHandler;
	private IDocumentNode fRootNode;
	private IDocumentNode[] fCachedChildren;
	
	public DocumentModel() {
		super();
		fParser= XMLCore.getDefault().createXMLModelBuilder(new DocumentModelFactory());
		fErrorHandler= new XEErrorHandler(null);
		fParser.setErrorHandler(fErrorHandler);
	}
	
	public void load(InputStream stream) {
		reconcile(stream);		
	}
	
	public void reconcile(InputStream stream) {
		try {
			fErrorHandler.reset();
			fParser.parse(new XMLInputSource(null, null, null, stream, null));
		} catch (Exception e) {
		}
		fCachedChildren = null;
	}
	
	public void setRootNode(IDocumentNode root) {
		fRootNode= root;
		if (fRootNode != null)
			fRootNode.setParent(this);
	}
	
	public IDocumentNode getRootNode() {
		fRootNode = fParser.getModelRoot();
		return fRootNode;
	}
	
	public IDocumentNode[] getChildren() {
		if (fCachedChildren == null)
			fCachedChildren = getRootNode().getChildren();
		return fCachedChildren;
	}
	
	public IDocumentNode getParent() {
		return null;
	}
	
	public void setParent(IDocumentNode parentNode) {
		throw new UnsupportedOperationException();
	}

	public ISourceRange getSourceRange() {
		return (ISourceRange)fParser.getLineTable().get(getRootNode());
	}
		
	public String getText() {
		return getTagName();
	}
	
	public String getTagName() {
		return (fRootNode != null) ? fRootNode.getText() : "";		
	}
	
	public Node getContent() {
		return fRootNode.getContent();
	}

}
