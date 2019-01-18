/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.plugin;

import java.io.StringReader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PluginHandler extends DefaultHandler {
	private Document fDocument;
	private Element fRootElement;
	private final Stack<Element> fOpenElements = new Stack<>();

	private String fSchemaVersion;
	private final boolean fAbbreviated;
	private Locator fLocator;
	private boolean fPop;

	private static final Pattern VERSION_RE = Pattern.compile("version\\s*=\\s*\"([^\"]+)\""); //$NON-NLS-1$

	public PluginHandler(boolean abbreviated) {
		fAbbreviated = abbreviated;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		fPop = true;

		if (fAbbreviated && fOpenElements.size() == 2) {
			Element parent = fOpenElements.peek();
			if (parent.getNodeName().equals("extension") && !isInterestingExtension(fOpenElements.peek())) { //$NON-NLS-1$
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

		if (fRootElement == null) {
			fRootElement = element;
		} else {
			fOpenElements.peek().appendChild(element);
		}

		fOpenElements.push(element);
	}

	protected boolean isInterestingExtension(Element element) {
		String point = element.getAttribute("point"); //$NON-NLS-1$
		return IdUtil.isInterestingExtensionPoint(point);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (fPop || (qName.equals("extension") && fOpenElements.size() == 2)) { //$NON-NLS-1$
			fOpenElements.pop();
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	@Override
	public void startDocument() throws SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			fDocument = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
		}
	}

	@Override
	public void endDocument() throws SAXException {
		fDocument.appendChild(fRootElement);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			// Data should be of the form: version="<version>"
			data = data.trim();
			Matcher matcher = VERSION_RE.matcher(data);
			if (matcher.matches()) {
				fSchemaVersion = TargetPlatformHelper.getSchemaVersionForTargetVersion(matcher.group(1));
			} else {
				fSchemaVersion = TargetPlatformHelper.getSchemaVersion();
			}
		}
	}

	@Override
	public void characters(char[] characters, int start, int length) throws SAXException {
		if (fAbbreviated) {
			return;
		}

		processCharacters(characters, start, length);
	}

	/**
	 * @param characters
	 * @param start
	 * @param length
	 * @throws DOMException
	 */
	protected void processCharacters(char[] characters, int start, int length) throws DOMException {
		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < length; i++) {
			buff.append(characters[start + i]);
		}
		Text text = fDocument.createTextNode(buff.toString());
		if (fRootElement == null) {
			fDocument.appendChild(text);
		} else {
			fOpenElements.peek().appendChild(text);
		}
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

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
