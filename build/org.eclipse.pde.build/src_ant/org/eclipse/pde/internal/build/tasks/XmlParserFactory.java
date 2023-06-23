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
package org.eclipse.pde.internal.build.tasks;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class XmlParserFactory {
	private XmlParserFactory() {
		// static Utility only
	}

	private static final SAXParserFactory SAX_FACTORY_ERROR_ON_DOCTYPE_NS = createSAXFactoryWithErrorOnDOCTYPE();

	private static synchronized SAXParserFactory createSAXFactoryWithErrorOnDOCTYPE() {
		SAXParserFactory f = SAXParserFactory.newInstance();
		f.setNamespaceAware(true);
		try {
			// force org.xml.sax.SAXParseException for any DOCTYPE:
			f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	public static SAXParser createNsAwareSAXParserWithErrorOnDOCTYPE() throws ParserConfigurationException, SAXException {
		return SAX_FACTORY_ERROR_ON_DOCTYPE_NS.newSAXParser();
	}

}
