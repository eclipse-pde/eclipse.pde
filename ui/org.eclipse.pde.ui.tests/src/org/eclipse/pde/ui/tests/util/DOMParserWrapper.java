/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.tests.util;

import org.eclipse.pde.internal.core.util.PDEXMLHelper;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DOMParserWrapper {

	protected DocumentBuilder fParser;
	protected boolean isdisposed;

	/**
	 * 
	 */
	public DOMParserWrapper() throws ParserConfigurationException, FactoryConfigurationError {
		fParser = PDEXMLHelper.Instance().getDefaultDOMParser();
		isdisposed = false;
	}

	// Explicit disposal
	public void dispose() {
		if (isdisposed == false) {
			PDEXMLHelper.Instance().recycleDOMParser(fParser);
			isdisposed = true;
		}
	}

	public Document parse(File f) throws SAXException, IOException {
		return fParser.parse(f);
	}

	public Document newDocument() {
		return fParser.newDocument();
	}

	// NOTE:  If other parser method calls are required, the corresponding
	// wrapper method needs to be added here

	// Explicit disposal	
	protected void finalize() throws Throwable {
		super.finalize();
		dispose();
	}

}
