/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.NodeDocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * SimpleCSDocumentHandler
 *
 */
public class SimpleCSDocumentHandler extends NodeDocumentHandler {

	private SimpleCSModel fModel;
	
	private String fCollapsibleParentName;
	
	/**
	 * @param reconciling
	 */
	public SimpleCSDocumentHandler(SimpleCSModel model, boolean reconciling) {
		super(reconciling, model.getFactory());
		fModel = model;
		fCollapsibleParentName = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.NodeDocumentHandler#getRootNode()
	 */
	protected IDocumentElementNode getRootNode() {
		return (IDocumentElementNode)fModel.getSimpleCS();
	}
	
	/**
	 * @param tagName
	 */
	private void setCollapsibleParentName(String tagName) {
		fCollapsibleParentName = tagName;
	}
	
	/**
	 * @return
	 */
	private String getCollapsibleParentName() {
		return fCollapsibleParentName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		
		IDocumentElementNode parent = getLastParsedDocumentNode();
		if ((parent != null) &&
				(parent.isContentCollapsed() == true)) {
			setCollapsibleParentName(parent.getXMLTagName());
			processCollapsedStartElement(name, attributes, parent);
		} else {
			super.startElement(uri, localName, name, attributes);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		
		if ((getCollapsibleParentName() != null) &&
				(getCollapsibleParentName().equals(name))) {
			setCollapsibleParentName(null);
		}
	
		if ((getCollapsibleParentName() != null)) {
			IDocumentElementNode parent = getLastParsedDocumentNode();
			processCollapsedEndElement(name, parent);
		} else {
			super.endElement(uri, localName, name);
		}		
	}
	
	/**
	 * @param name
	 * @param parent
	 */
	private void processCollapsedEndElement(String name, IDocumentElementNode parent) {
		// Get the document node
		IDocumentElementNode node = getDocumentNode(name, parent);
		// If the start element is self-terminating, no end tag is required
		boolean terminate = node.canTerminateStartTag();
		if (terminate) {
			return;
		}
		// Serialize the document node XML end tag
		StringBuffer endElementString = new StringBuffer();
		endElementString.append('<');
		endElementString.append('/');
		endElementString.append(name);
		endElementString.append('>');
		// Set the XML end tag string as text in the text node
		getDocumentTextNode(endElementString.toString(), parent);
	}

	/**
	 * @param name
	 * @param attributes
	 * @param parent
	 */
	private void processCollapsedStartElement(String name, Attributes attributes,
			IDocumentElementNode parent) {
		// Create the document node
		IDocumentElementNode node = getDocumentNode(name, parent);
		// Create the attributes
		for (int i = 0; i < attributes.getLength(); i++) {
			String attName = attributes.getQName(i);
			String attValue = attributes.getValue(i);
			IDocumentAttributeNode attribute = getDocumentAttribute(attName, attValue, node);
			if (attribute != null) {
				node.setXMLAttribute(attribute);
			}
		}
		// Serialize the document node XML start tag
		boolean terminate = node.canTerminateStartTag();
		String startElementString = node.writeShallow(terminate);
		// Set the XML start tag string as text in the text node
		getDocumentTextNode(startElementString, parent);
	}
	
}
