/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * PDEXMLHelper
 *
 */
public class PDEXMLHelper {

	protected static SAXParserFactory fSAXFactory;
	protected static PDEXMLHelper fPinstance;
	protected static DocumentBuilderFactory fDOMFactory;
	protected static List<SoftReference<SAXParser>> fSAXParserQueue;
	protected static List<SoftReference<DocumentBuilder>> fDOMParserQueue;
	protected static int fSAXPoolLimit;
	protected static int fDOMPoolLimit;
	protected static final int FMAXPOOLLIMIT = 1;

	protected PDEXMLHelper() throws FactoryConfigurationError {
		fSAXFactory = SAXParserFactory.newInstance();
		fDOMFactory = DocumentBuilderFactory.newInstance();
		fSAXParserQueue = Collections.synchronizedList(new LinkedList<SoftReference<SAXParser>>());
		fDOMParserQueue = Collections.synchronizedList(new LinkedList<SoftReference<DocumentBuilder>>());
		fSAXPoolLimit = FMAXPOOLLIMIT;
		fDOMPoolLimit = FMAXPOOLLIMIT;
	}

	public synchronized SAXParser getDefaultSAXParser() throws ParserConfigurationException, SAXException {

		SAXParser parser = null;
		if (fSAXParserQueue.isEmpty()) {
			parser = fSAXFactory.newSAXParser();
		} else {
			SoftReference<?> reference = fSAXParserQueue.remove(0);
			if (reference.get() != null) {
				parser = (SAXParser) reference.get();
			} else {
				parser = fSAXFactory.newSAXParser();
			}
		}
		return parser;
	}

	public synchronized DocumentBuilder getDefaultDOMParser() throws ParserConfigurationException {

		DocumentBuilder parser = null;
		if (fDOMParserQueue.isEmpty()) {
			parser = fDOMFactory.newDocumentBuilder();
		} else {
			SoftReference<?> reference = fDOMParserQueue.remove(0);
			if (reference.get() != null) {
				parser = (DocumentBuilder) reference.get();
			} else {
				parser = fDOMFactory.newDocumentBuilder();
			}
		}
		return parser;
	}

	public static PDEXMLHelper Instance() throws FactoryConfigurationError {
		if (fPinstance == null) {
			fPinstance = new PDEXMLHelper();
		}
		return fPinstance;
	}

	public synchronized void recycleSAXParser(SAXParser parser) {
		if (fSAXParserQueue.size() < fSAXPoolLimit) {
			SoftReference<SAXParser> reference = new SoftReference<>(parser);
			fSAXParserQueue.add(reference);
		}
	}

	public synchronized void recycleDOMParser(DocumentBuilder parser) {
		if (fDOMParserQueue.size() < fDOMPoolLimit) {
			SoftReference<DocumentBuilder> reference = new SoftReference<>(parser);
			fDOMParserQueue.add(reference);
		}
	}

	public static String getWritableString(String source) {
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	public static String getWritableAttributeString(String source) {
		// Ensure source is defined
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		// Trim the leading and trailing whitespace if any
		source = source.trim();
		// Translate source using a buffer
		StringBuilder buffer = new StringBuilder();
		// Translate source character by character
		for (int i = 0; i < source.length(); i++) {
			char character = source.charAt(i);
			switch (character) {
				case '&' :
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buffer.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buffer.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buffer.append("&quot;"); //$NON-NLS-1$
					break;
				case '\r' :
					buffer.append("&#x0D;"); //$NON-NLS-1$
					break;
				case '\n' :
					buffer.append("&#x0A;"); //$NON-NLS-1$
					break;
				default :
					buffer.append(character);
					break;
			}
		}
		return buffer.toString();
	}

	public static int getSAXPoolLimit() {
		return fSAXPoolLimit;
	}

	public static void setSAXPoolLimit(int poolLimit) {
		fSAXPoolLimit = poolLimit;
	}

	public static int getDOMPoolLimit() {
		return fDOMPoolLimit;
	}

	public static void setDOMPoolLimit(int poolLimit) {
		fDOMPoolLimit = poolLimit;
	}

}
