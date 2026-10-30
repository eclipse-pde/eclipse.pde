/*******************************************************************************
 *  Copyright (c) 2003, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.StringReader;
import java.util.Stack;

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
	private StringBuilder fText;

	protected Stack<Element> fElementStack = new Stack<>();
	protected boolean fAbbreviated;

	public XMLDefaultHandler() {
	}

	public XMLDefaultHandler(boolean abbreviated) {
		fAbbreviated = abbreviated;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (!isPrepared()) {
			return;
		}
		appendText();
		Element element = fDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}

		if (fRootElement == null) {
			fRootElement = element;
		} else {
			fElementStack.peek().appendChild(element);
		}
		fElementStack.push(element);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (isPrepared() && !fElementStack.isEmpty()) {
			appendText();
			fElementStack.pop();
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	@SuppressWarnings("restriction")
	public void startDocument() throws SAXException {
		try {
			fDocument = org.eclipse.core.internal.runtime.XmlProcessorFactory.newDocumentWithErrorOnDOCTYPE();
		} catch (ParserConfigurationException e) {
		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (isPrepared()) {
			appendText();
			fDocument.appendChild(fRootElement);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (isPrepared()) {
			fDocument.appendChild(fDocument.createProcessingInstruction(target, data));
		}
	}

	@Override
	public void characters(char[] characters, int start, int length) throws SAXException {
		if (fAbbreviated || !isPrepared() || length == 0) {
			return;
		}
		if (fText == null) {
			fText = new StringBuilder(length);
		}
		// SAX can split one text node across many callbacks. Collect its content
		// before creating the node to avoid repeatedly concatenating it in normalize().
		fText.append(characters, start, length);
	}

	private void appendText() {
		if (fText == null || fText.length() == 0) {
			return;
		}
		Text text = fDocument.createTextNode(fText.toString());
		if (fRootElement == null) {
			fDocument.appendChild(text);
		} else {
			fElementStack.peek().appendChild(text);
		}
		fText.setLength(0);
	}

	public Node getDocumentElement() {
		if (!isPrepared()) {
			return null;
		}
		normalizeDocumentElement();
		return fDocument.getDocumentElement();
	}

	public org.w3c.dom.Document getDocument() {
		if (!isPrepared()) {
			return null;
		}
		normalizeDocumentElement();
		return fDocument;
	}

	public boolean isPrepared() {
		return fDocument != null;
	}

	private void normalizeDocumentElement() {
		if (fDocument.getDocumentElement() != null) {
			fDocument.getDocumentElement().normalize();
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
