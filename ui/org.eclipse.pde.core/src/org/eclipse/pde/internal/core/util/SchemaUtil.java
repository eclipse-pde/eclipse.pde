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

package org.eclipse.pde.internal.core.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SchemaUtil
 *
 */
public class SchemaUtil {

	public static InputStream getInputStream(URL url) throws IOException {
		if (url == null) {
			throw new MalformedURLException("URL specified is null"); //$NON-NLS-1$
		} else if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
			return new BufferedInputStream(new FileInputStream(url.getFile()));
		}
		return new BufferedInputStream(url.openStream());
	}

	public static void parseURL(URL url, DefaultHandler handler) {
		InputStream input = null;
		try {
			input = getInputStream(url);
			SAXParserWrapper parser = new SAXParserWrapper();
			parser.parse(input, handler);
		} catch (MalformedURLException e) {
			// Ignore
			// Caused when URL is null
			// This occurs when the extension point schema is first
			// created.
		} catch (IOException e) {
			PDECore.logException(e);
		} catch (SAXException e) {
			// Ignore parse errors
			// Handler may send a SAX Exception to prematurely abort parsing
			// in order to save execution time.  This is not an error
		} catch (ParserConfigurationException e) {
			PDECore.logException(e);
		} catch (FactoryConfigurationError e) {
			PDECore.logException(e);
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e1) {
			}
		}
	}

}
