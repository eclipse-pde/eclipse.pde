/*******************************************************************************
 *  Copyright (c) 2023 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlParserFactory {
	private XmlParserFactory() {
		// static Utility only
	}

	private static final SAXParserFactory SAX_FACTORY_ERROR_ON_DOCTYPE = createSAXFactoryWithErrorOnDOCTYPE(false);
	private static final SAXParserFactory SAX_FACTORY_ERROR_ON_DOCTYPE_NS = createSAXFactoryWithErrorOnDOCTYPE(true);
	private static final SAXParserFactory SAX_FACTORY_IGNORING_DOCTYPE = createSAXFactoryIgnoringDOCTYPE();

	private static synchronized SAXParserFactory createSAXFactoryWithErrorOnDOCTYPE(boolean awareness) {
		SAXParserFactory f = SAXParserFactory.newInstance();
		f.setNamespaceAware(awareness);
		try {
			// force org.xml.sax.SAXParseException for any DOCTYPE:
			f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	private static synchronized SAXParserFactory createSAXFactoryIgnoringDOCTYPE() {
		SAXParserFactory f = SAXParserFactory.newInstance();
		try {
			// ignore DOCTYPE:
			f.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			f.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
			f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	public static SAXParser createSAXParserWithErrorOnDOCTYPE() throws ParserConfigurationException, SAXException {
		return createSAXParserWithErrorOnDOCTYPE(false);
	}

	public static SAXParser createSAXParserWithErrorOnDOCTYPE(boolean awareness)
			throws ParserConfigurationException, SAXException {
		if (awareness) {
			return SAX_FACTORY_ERROR_ON_DOCTYPE_NS.newSAXParser();
		}
		return SAX_FACTORY_ERROR_ON_DOCTYPE.newSAXParser();
	}

	public static SAXParser createSAXParserIgnoringDOCTYPE()
			throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException, SAXException {
		SAXParser parser = SAX_FACTORY_IGNORING_DOCTYPE.newSAXParser();
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); //$NON-NLS-1$
		return parser;
	}

}
