/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

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
			return new FileInputStream(url.getFile());
		}
		return url.openStream();
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

	/**
	 * Method created to compensate for Sun Crimson SAX parser bug contained in 
	 * SUN JDK 1.4.2
	 * SAX event callback for characters method returns a bogus length value
	 * i.e. When '\r' (carriage return) is the first character after the '<',
	 * the Sun parser doesn't normalize the carriage return into a '\n' line
	 * feed and returns a zero length
	 * i.e. When '&lt' (less than) is in the character range, the length is only
	 * measured up to it
	 * 
	 * This method calculates the length manually and returns the characters
	 * within the specified proper range.
	 * 
	 * Assumption:  The element from which characters are being retrieved do
	 * not contain mixed content (i.e. PCDATA and other elements)
	 * Note: Noticed that predefined general entity references are not being
	 * resolved i.e. &lt; = '<'.  Appears to be another parser bug.
	 * @param endElementName
	 * @param ch
	 * @param start
	 * @param length
	 * @return
	 */	
	public static String getCharacters(String endElementName, char[] ch, int start, int length) {
		char[] endElement = endElementName.toCharArray();
		StringBuffer buff = new StringBuffer();
		boolean endFlag = true;
		// Scan until we find the end element
		for (int i = start; i < ch.length; i++) {
			// Check for first character of end element tag
			if (ch[i] == '<') {
				// Potential match found
				// Check to see if we can safely scan ahead in the character array
				if ((endElement.length + 1 + i) < ch.length) {
					// Check for second character of end element tag
					if (ch[i + 1] == '/') {
						int offset = i + 2;
						// Check to see if the end element tag name matches
						for (int j = 0; j < endElement.length; j++) {
							if (endElement[j] != ch[offset + j]) {
								// Wrong end element, abort
								endFlag = false;
								break;
							}
						}
						if (endFlag) {
							// Return what we have in the buffer so far
							return buff.toString();
						}
					}
				} else {
					// If we can't scan ahead, that means the XML document is
					// not well-formed
					return null;
				}
			}
			// Store characters in the buffer until the end element tag is found
			// If it isn't found the buffer is discarded
			buff.append(ch[i]);
		}
		// We past the end of the array without finding an end element tag
		// Return nothing
		return null;
	}
	
}
