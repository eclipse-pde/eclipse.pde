/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public abstract class NodeDocumentHandler extends DocumentHandler {

	private IDocumentNodeFactory fFactory;
	protected String fCollapsibleParentName;

	// TODO: MP: TEO: LOW: Make PluginDocumentHandler extend this

	/**
	 * @param reconciling
	 */
	public NodeDocumentHandler(boolean reconciling, IDocumentNodeFactory factory) {
		super(reconciling);
		fFactory = factory;
	}

	protected IDocumentNodeFactory getFactory() {
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocument()
	 */
	protected abstract IDocument getDocument();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent) {
		IDocumentAttributeNode attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = fFactory.createAttribute(name, value, parent);
			} else {
				if (!name.equals(attr.getAttributeName()))
					attr.setAttributeName(name);
				if (!value.equals(attr.getAttributeValue()))
					attr.setAttributeValue(value);
			}
		} catch (CoreException e) {
		}
		return attr;
	}

	protected abstract IDocumentElementNode getRootNode();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentElementNode getDocumentNode(String name, IDocumentElementNode parent) {
		IDocumentElementNode node = null;
		if (parent == null) {
			node = getRootNode();
			if (node != null) {
				node.setOffset(-1);
				node.setLength(-1);
			}
		} else {
			IDocumentElementNode[] children = parent.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i].getOffset() < 0) {
					if (name.equals(children[i].getXMLTagName())) {
						node = children[i];
					}
					break;
				}
			}
		}

		if (node == null)
			return fFactory.createDocumentNode(name, parent);

		IDocumentAttributeNode[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			attrs[i].setNameOffset(-1);
			attrs[i].setNameLength(-1);
			attrs[i].setValueOffset(-1);
			attrs[i].setValueLength(-1);
		}

		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentElementNode child = node.getChildAt(i);
			child.setOffset(-1);
			child.setLength(-1);
		}

		// clear text nodes if the user is typing on the source page
		// they will be recreated in the characters() method
		if (isReconciling()) {
			node.removeTextNode();
			node.setIsErrorNode(false);
		}

		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocumentTextNode()
	 */
	protected IDocumentTextNode getDocumentTextNode(String content, IDocumentElementNode parent) {

		IDocumentTextNode textNode = parent.getTextNode();
		if (textNode == null) {
			if (content.trim().length() > 0) {
				textNode = fFactory.createDocumentTextNode(content, parent);
			}
		} else {
			String newContent = textNode.getText() + content;
			textNode.setText(newContent);
		}
		return textNode;
	}

	/**
	 * @param tagName
	 */
	protected void setCollapsibleParentName(String tagName) {
		fCollapsibleParentName = tagName;
	}

	protected String getCollapsibleParentName() {
		return fCollapsibleParentName;
	}

	protected void processCollapsedEndElement(String name, IDocumentElementNode parent) {
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

	protected void processCollapsedStartElement(String name, Attributes attributes, IDocumentElementNode parent) {
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

	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		IDocumentElementNode parent = getLastParsedDocumentNode();
		if ((parent != null) && (parent.isContentCollapsed() == true)) {
			setCollapsibleParentName(parent.getXMLTagName());
			processCollapsedStartElement(name, attributes, parent);
		} else {
			super.startElement(uri, localName, name, attributes);
		}
	}

	public void endElement(String uri, String localName, String name) throws SAXException {

		if ((getCollapsibleParentName() != null) && (getCollapsibleParentName().equals(name))) {
			setCollapsibleParentName(null);
		}

		if ((getCollapsibleParentName() != null)) {
			IDocumentElementNode parent = getLastParsedDocumentNode();
			processCollapsedEndElement(name, parent);
		} else {
			super.endElement(uri, localName, name);
		}
	}

}
