/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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

	/**
	 * Returns a URL connection that an input stream can be obtained from.  The
	 * URL Connection can handle urls of a variety of types including files, jar
	 * files and remote urls.
	 * <p>
	 * NOTE: If the connection is of type {@link JarURLConnection} the zip file
	 * should be independantly closed using {@link JarURLConnection#getJarFile()}.close()
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=326263
	 * </p>
	 *
	 * @param url URL to open connection to
	 * @return the url connection
	 * @throws MalformedURLException if the url is null
	 * @throws IOException if there is a problem accessing the resource specified by the url
	 */
	public static URLConnection getURLConnection(URL url) throws MalformedURLException, IOException {
		if (url == null) {
			throw new MalformedURLException("URL specified is null"); //$NON-NLS-1$
		}
		URLConnection connection = url.openConnection();
		if (connection instanceof JarURLConnection) {
			connection.setUseCaches(false);
		}
		return connection;
	}

	public static void parseURL(URL url, DefaultHandler handler) {
		URLConnection connection = null;
		try {
			connection = getURLConnection(url);
			try (InputStream input = connection.getInputStream()) {
				SAXParserWrapper parser = new SAXParserWrapper();
				parser.parse(input, handler);
			}
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
				if (connection instanceof JarURLConnection) {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=326263
					((JarURLConnection) connection).getJarFile().close();
				}
			} catch (IOException e1) {
			}
		}
	}
}
