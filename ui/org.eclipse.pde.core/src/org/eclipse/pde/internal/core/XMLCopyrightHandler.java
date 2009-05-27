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
package org.eclipse.pde.internal.core;

import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XMLCopyrightHandler implements LexicalHandler {

	private String fCopyright = null;
	private XMLDefaultHandler fHandler = null;

	public XMLCopyrightHandler(XMLDefaultHandler handler) {
		fHandler = handler;
	}

	public void comment(char[] ch, int start, int length) throws SAXException {
		// if we haven't parsed any elements, we assume it is a copyright
		if (fHandler != null && fCopyright == null && fHandler.fElementStack.isEmpty()) {
			fCopyright = new String(ch, start, length);
		}
	}

	public void endCDATA() throws SAXException {
	}

	public void endDTD() throws SAXException {
	}

	public void endEntity(String name) throws SAXException {
	}

	public void startCDATA() throws SAXException {
	}

	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	public void startEntity(String name) throws SAXException {
	}

	public String getCopyright() {
		return fCopyright;
	}

}
