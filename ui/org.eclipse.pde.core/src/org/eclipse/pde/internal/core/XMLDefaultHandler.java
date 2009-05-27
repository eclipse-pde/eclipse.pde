/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.StringReader;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLDefaultHandler extends DefaultHandler {

	private org.w3c.dom.Document fDocument;
	private Element fRootElement;

	protected Stack fElementStack = new Stack();
	protected boolean fAbbreviated;

	public XMLDefaultHandler() {
	}

	public XMLDefaultHandler(boolean abbreviated) {
		fAbbreviated = abbreviated;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (!isPrepared())
			return;
		Element element = fDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}

		if (fRootElement == null)
			fRootElement = element;
		else
			((Element) fElementStack.peek()).appendChild(element);
		fElementStack.push(element);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (isPrepared() && !fElementStack.isEmpty())
			fElementStack.pop();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			fDocument = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		if (isPrepared())
			fDocument.appendChild(fRootElement);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		if (isPrepared())
			fDocument.appendChild(fDocument.createProcessingInstruction(target, data));
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		if (fAbbreviated || !isPrepared())
			return;
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fDocument.createTextNode(buff.toString());
		if (fRootElement == null)
			fDocument.appendChild(text);
		else
			((Element) fElementStack.peek()).appendChild(text);
	}

	public Node getDocumentElement() {
		if (!isPrepared())
			return null;
		normalizeDocumentElement();
		return fDocument.getDocumentElement();
	}

	public org.w3c.dom.Document getDocument() {
		if (!isPrepared())
			return null;
		normalizeDocumentElement();
		return fDocument;
	}

	public boolean isPrepared() {
		return fDocument != null;
	}

	private void normalizeDocumentElement() {
		if (fDocument.getDocumentElement() != null)
			fDocument.getDocumentElement().normalize();
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
