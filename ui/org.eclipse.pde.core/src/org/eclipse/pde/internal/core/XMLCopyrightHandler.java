/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XMLCopyrightHandler implements LexicalHandler {

	private String fCopyright = null;
	private XMLDefaultHandler fHandler = null;

	public XMLCopyrightHandler(XMLDefaultHandler handler) {
		fHandler = handler;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		// if we haven't parsed any elements, we assume it is a copyright
		if (fHandler != null && fCopyright == null && fHandler.fElementStack.isEmpty()) {
			fCopyright = new String(ch, start, length);
		}
	}

	@Override
	public void endCDATA() throws SAXException {
	}

	@Override
	public void endDTD() throws SAXException {
	}

	@Override
	public void endEntity(String name) throws SAXException {
	}

	@Override
	public void startCDATA() throws SAXException {
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	@Override
	public void startEntity(String name) throws SAXException {
	}

	public String getCopyright() {
		return fCopyright;
	}

}
