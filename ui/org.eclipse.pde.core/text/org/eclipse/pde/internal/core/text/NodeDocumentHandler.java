/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private final IDocumentNodeFactory fFactory;
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

	@Override
	protected abstract IDocument getDocument();

	@Override
	protected IDocumentAttributeNode getDocumentAttribute(String name, String value, IDocumentElementNode parent) {
		IDocumentAttributeNode attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = fFactory.createAttribute(name, value, parent);
			} else {
				if (!name.equals(attr.getAttributeName())) {
					attr.setAttributeName(name);
				}
				if (!value.equals(attr.getAttributeValue())) {
					attr.setAttributeValue(value);
				}
			}
		} catch (CoreException e) {
		}
		return attr;
	}

	protected abstract IDocumentElementNode getRootNode();

	@Override
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
			for (IDocumentElementNode childNode : children) {
				if (childNode.getOffset() < 0) {
					if (name.equals(childNode.getXMLTagName())) {
						node = childNode;
					}
					break;
				}
			}
		}

		if (node == null) {
			return fFactory.createDocumentNode(name, parent);
		}

		IDocumentAttributeNode[] attrs = node.getNodeAttributes();
		for (IDocumentAttributeNode attrNode : attrs) {
			attrNode.setNameOffset(-1);
			attrNode.setNameLength(-1);
			attrNode.setValueOffset(-1);
			attrNode.setValueLength(-1);
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

	@Override
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
		StringBuilder endElementString = new StringBuilder();
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

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		IDocumentElementNode parent = getLastParsedDocumentNode();
		if ((parent != null) && (parent.isContentCollapsed() == true)) {
			setCollapsibleParentName(parent.getXMLTagName());
			processCollapsedStartElement(name, attributes, parent);
		} else {
			super.startElement(uri, localName, name, attributes);
		}
	}

	@Override
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
