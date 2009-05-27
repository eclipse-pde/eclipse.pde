/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.StringReader;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class PluginHandler extends DefaultHandler {
	private Document fDocument;
	private Element fRootElement;
	private Stack fOpenElements = new Stack();

	private String fSchemaVersion;
	private boolean fAbbreviated;
	private Locator fLocator;
	private boolean fPop;

	public PluginHandler(boolean abbreviated) {
		fAbbreviated = abbreviated;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		fPop = true;

		if (fAbbreviated && fOpenElements.size() == 2) {
			Element parent = (Element) fOpenElements.peek();
			if (parent.getNodeName().equals("extension") && !isInterestingExtension((Element) fOpenElements.peek())) { //$NON-NLS-1$
				fPop = false;
				return;
			}
		}

		Element element = fDocument.createElement(qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
			if ("extension".equals(qName) || "extension-point".equals(qName)) { //$NON-NLS-1$ //$NON-NLS-2$
				element.setAttribute("line", Integer.toString(fLocator.getLineNumber())); //$NON-NLS-1$
			}
		}

		if (fRootElement == null)
			fRootElement = element;
		else
			((Element) fOpenElements.peek()).appendChild(element);

		fOpenElements.push(element);
	}

	protected boolean isInterestingExtension(Element element) {
		String point = element.getAttribute("point"); //$NON-NLS-1$
		return IdUtil.isInterestingExtensionPoint(point);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (fPop || (qName.equals("extension") && fOpenElements.size() == 2)) { //$NON-NLS-1$
			fOpenElements.pop();
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
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
		fDocument.appendChild(fRootElement);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			// Data should be of the form: version="<version>"
			if (data.length() > 10 && data.substring(0, 9).equals("version=\"") && data.charAt(data.length() - 1) == '\"') { //$NON-NLS-1$
				fSchemaVersion = TargetPlatformHelper.getSchemaVersionForTargetVersion(data.substring(9, data.length() - 1));
			} else {
				fSchemaVersion = TargetPlatformHelper.getSchemaVersion();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
		if (fAbbreviated)
			return;

		processCharacters(characters, start, length);
	}

	/**
	 * @param characters
	 * @param start
	 * @param length
	 * @throws DOMException
	 */
	protected void processCharacters(char[] characters, int start, int length) throws DOMException {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fDocument.createTextNode(buff.toString());
		if (fRootElement == null)
			fDocument.appendChild(text);
		else
			((Element) fOpenElements.peek()).appendChild(text);
	}

	public Node getDocumentElement() {
		if (fRootElement != null) {
			fRootElement.normalize();
		}
		return fRootElement;
	}

	public String getSchemaVersion() {
		return fSchemaVersion;
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
