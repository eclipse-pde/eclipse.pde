/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.schema;

import java.io.StringReader;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * BaseDescriptionHandler
 *
 */
public class BaseSchemaHandler extends DefaultHandler {

	protected LinkedList fElementList;

	public BaseSchemaHandler() {
		reset();
	}

	protected void reset() {
		fElementList = new LinkedList();
	}

	public void startDocument() throws SAXException {
		reset();
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// Track where we are in the XML document
		// Note:  XML namespaces not utilized, safe to use qualified name
		fElementList.addFirst(qName);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		// Track where we are in the XML document
		if (fElementList.size() != 0) {
			fElementList.removeFirst();
		} else {
			// This should never happened and is ignored in any case
			throw new SAXException("Serious error.  XML document is not well-formed"); //$NON-NLS-1$
		}
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		// Prevent the resolution of external entities in order to
		// prevent the parser from accessing the Internet
		// This will prevent huge workbench performance degradations and hangs
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
