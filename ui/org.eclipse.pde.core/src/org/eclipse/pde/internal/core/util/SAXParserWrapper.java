/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAXParserWrapper
 *
 */
public class SAXParserWrapper {

	protected SAXParser fParser;
	protected boolean isdisposed;

	/**
	 * 
	 */
	public SAXParserWrapper() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
		fParser = PDEXMLHelper.Instance().getDefaultSAXParser();
		isdisposed = false;
	}

	// Explicit disposal
	public void dispose() {
		if (isdisposed == false) {
			PDEXMLHelper.Instance().recycleSAXParser(fParser);
			isdisposed = true;
		}
	}

	public void parse(File f, DefaultHandler dh) throws SAXException, IOException {
		fParser.parse(f, dh);
	}

	public void parse(InputStream is, DefaultHandler dh) throws SAXException, IOException {
		fParser.parse(is, dh);
	}

	public void parse(InputSource is, DefaultHandler dh) throws SAXException, IOException {
		fParser.parse(is, dh);
	}

	// NOTE:  If other parser method calls are required, the corresponding
	// wrapper method needs to be added here

	// Implicit disposal
	protected void finalize() throws Throwable {
		super.finalize();
		dispose();
	}

}
