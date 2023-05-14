/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAXParserWrapper
 *
 */
public class SAXParserWrapper {

	private SAXParserWrapper() { // static use only
	}

	public static void parse(File f, DefaultHandler dh)
			throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
		parse(p -> p.parse(f, dh));
	}

	public static void parse(InputStream is, DefaultHandler dh)
			throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
		parse(p -> p.parse(is, dh));
	}

	// NOTE: If other parser method calls are required, the corresponding
	// wrapper method needs to be added here

	private interface ParseProcess {
		void parseWith(SAXParser parser) throws SAXException, IOException;
	}

	private static void parse(ParseProcess pp)
			throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
		SAXParser fParser = PDEXMLHelper.Instance().getDefaultSAXParser();
		try {
			pp.parseWith(fParser);
		} finally {
			PDEXMLHelper.Instance().recycleSAXParser(fParser);
		}
	}
}
