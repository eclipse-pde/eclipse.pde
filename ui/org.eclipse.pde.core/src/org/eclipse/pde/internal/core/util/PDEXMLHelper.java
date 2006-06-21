/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
	protected static List fSAXParserQueue;
	protected static List fDOMParserQueue;
	protected static int fSAXPoolLimit;
	protected static int fDOMPoolLimit;
	protected static final int FMAXPOOLLIMIT = 1;
	
	protected PDEXMLHelper() throws FactoryConfigurationError {
		fSAXFactory = SAXParserFactory.newInstance();
		fDOMFactory = DocumentBuilderFactory.newInstance();
		fSAXParserQueue = Collections.synchronizedList(new LinkedList());
		fDOMParserQueue = Collections.synchronizedList(new LinkedList());
		fSAXPoolLimit = FMAXPOOLLIMIT;
		fDOMPoolLimit = FMAXPOOLLIMIT;
	}
	
	public synchronized SAXParser getDefaultSAXParser() throws ParserConfigurationException, SAXException {

		SAXParser parser = null;
		if (fSAXParserQueue.isEmpty()) {
			parser = fSAXFactory.newSAXParser();
		} else {
			SoftReference reference = (SoftReference)fSAXParserQueue.remove(0);
			if (reference.get() != null) {
				parser = (SAXParser)reference.get();
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
			SoftReference reference = (SoftReference)fDOMParserQueue.remove(0);
			if (reference.get() != null) {
				parser = (DocumentBuilder)reference.get();
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
			SoftReference reference = new SoftReference(parser);
			fSAXParserQueue.add(reference);
		}
	}

	public synchronized void recycleDOMParser(DocumentBuilder parser) {
		if (fDOMParserQueue.size() < fDOMPoolLimit) {		
			SoftReference reference = new SoftReference(parser);
			fDOMParserQueue.add(reference);
		}
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
