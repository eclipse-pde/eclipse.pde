/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.util;

import java.io.*;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMLContentTypeHandler
 *
 */
public class XMLRootElementMatcher {
	public static boolean fileMatchesElement(IFile file, String element) {
		try {
			return matchFile(file.getContents(), element);
		} catch (CoreException e) {
			return false;
		}
	}

	public static boolean fileMatchesElement(File file, String element) {
		try {
			InputStream stream = new FileInputStream(file);
			return matchFile(stream, element);
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	private static boolean matchFile(InputStream stream, String element) {
		XMLContentTypeHandler handler = new XMLContentTypeHandler();
		try {
			SAXParserWrapper parser = new SAXParserWrapper();
			parser.parse(stream, handler);
		} catch (ParserConfigurationException e) {
			return false;
		} catch (AbortParseException e) {
			return handler.isRootType(element);
		} catch (SAXException e) {
			return false;
		} catch (FactoryConfigurationError e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		return handler.isRootType(element);
	}

	/**
	 * AbortParseException
	 *
	 */
	private static class AbortParseException extends SAXException {
		private static final long serialVersionUID = 1L;

		public AbortParseException() {
			super("Parsing operation forcibly aborted to save on performance time."); //$NON-NLS-1$
		}
	}

	private static class XMLContentTypeHandler extends DefaultHandler {
		private String fRootElem;

		/**
		 * 
		 */
		private XMLContentTypeHandler() {
			fRootElem = null;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			fRootElem = qName;
			// Only care about the root node
			// Abort parsing to save on performance
			throw new AbortParseException();
		}

		/**
		 * @return
		 */
		public boolean isRootType(String rootType) {
			return fRootElem != null && fRootElem.equals(rootType);
		}
	}
}
