/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.io.*;

import javax.xml.parsers.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.xml.sax.*;

public class ValidatingSAXParser {
	
	private static SAXParserFactory fFactory;
	
	public static void parse(IFile file, XMLErrorReporter reporter) {
		try {
			parse(file.getContents(), reporter);
		} catch (CoreException e) {
		}
	}
	
	public static void parse(InputStream is, XMLErrorReporter reporter) {
		try {
			getParser().parse(is, reporter);
		} catch (Exception e) {
		} 
	}
	
	private static SAXParser getParser()
		throws ParserConfigurationException, SAXException {
		if (fFactory == null) {
			fFactory = SAXParserFactory.newInstance();
		}
		return fFactory.newSAXParser();
	}
	
}
