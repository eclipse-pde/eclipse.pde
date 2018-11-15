/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.tests.util;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.*;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DOMParserWrapper implements AutoCloseable {

	protected DocumentBuilder fParser;
	protected boolean isdisposed;

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

	@Override
	public void close() {
		dispose();
	}

}
