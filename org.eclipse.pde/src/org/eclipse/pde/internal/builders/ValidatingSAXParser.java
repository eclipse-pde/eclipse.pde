/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.io.IOException;
import java.net.URL;

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ValidatingSAXParser {
	private SAXParser parser;
	public ValidatingSAXParser() {
		parser = new SAXParser();

		try {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature(
				"http://apache.org/xml/features/validation/dynamic",
				true);
		} catch (SAXException e) {
			PDE.log(e);
		}
	}
	
	public SAXParser getParser() {
		return parser;
	}
	
	public void setErrorHandler(ErrorHandler handler) {
		parser.setErrorHandler(handler);
	}

	public void parse(InputSource inputSource) throws SAXException, IOException {
		URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
		inputSource.setSystemId(dtdLocation.toString());
		parser.parse(inputSource);
	}
}
