/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;

import org.eclipse.pde.internal.core.util.CoreUtility;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ExtensionsHandler extends DefaultHandler {
	
	private Stack fOpenElements;

	private Locator fLocator;

	private Element fParent;

	public ExtensionsHandler(Element parent) {
		fParent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String,
	 *      java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (fOpenElements == null) {
			if ((qName.equals("plugin") || qName.equals("fragment"))) { //$NON-NLS-1$ //$NON-NLS-2$
				fOpenElements = new Stack();
			}
		} else if (fOpenElements.size() == 0) {
			if (qName.equals("extension")) { //$NON-NLS-1$
				createExtension(attributes);
			} else if (qName.equals("extension-point")) { //$NON-NLS-1$
				createExtensionPoint(attributes);
			}
		} else {
			createElement(qName, attributes);
		}
	}

	/**
	 * @param attributes
	 */
	private void createExtension(Attributes attributes) {
		Element extension = fParent.getOwnerDocument().createElement("extension"); //$NON-NLS-1$
		String point = attributes.getValue("point"); //$NON-NLS-1$
		if (point == null)
			return;
		extension.setAttribute("point", point); //$NON-NLS-1$
		
		String id = attributes.getValue("id"); //$NON-NLS-1$
		if (id != null)
			extension.setAttribute("id", id); //$NON-NLS-1$
		
		String name = attributes.getValue("name"); //$NON-NLS-1$
		if (name != null)
			extension.setAttribute("name", name); //$NON-NLS-1$
		
		extension.setAttribute("line", Integer.toString(fLocator.getLineNumber())); //$NON-NLS-1$

		fParent.appendChild(extension);
		
		if (CoreUtility.isInterestingExtensionPoint(point))
			fOpenElements.push(extension);
	}

	/**
	 * @param attributes
	 */
	private void createExtensionPoint(Attributes attributes) {
		Element extPoint = fParent.getOwnerDocument().createElement("extension-point"); //$NON-NLS-1$

		String id = attributes.getValue("id"); //$NON-NLS-1$
		if (id == null)
			return;
		extPoint.setAttribute("id", id); //$NON-NLS-1$
		
		String name = attributes.getValue("name"); //$NON-NLS-1$
		if (name == null)
			return;
		extPoint.setAttribute("name", name); //$NON-NLS-1$
		
		String schema = attributes.getValue("schema"); //$NON-NLS-1$
		if (schema != null)
			extPoint.setAttribute("schema", schema); //$NON-NLS-1$
		
		extPoint.setAttribute("line", Integer.toString(fLocator.getLineNumber())); //$NON-NLS-1$

		fParent.appendChild(extPoint);
	}

	private void createElement(String tagName, Attributes attributes) {
		Element element = fParent.getOwnerDocument().createElement(tagName);
		for (int i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
		((Element)fOpenElements.peek()).appendChild(element);
		fOpenElements.push(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (fOpenElements != null && !fOpenElements.isEmpty())
			fOpenElements.pop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

}
