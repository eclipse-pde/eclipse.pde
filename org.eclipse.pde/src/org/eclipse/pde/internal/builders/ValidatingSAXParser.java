/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		InputStream stream = null;
		try {
			stream = file.getContents();
			getParser().parse(stream, reporter);
		} catch (CoreException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}
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
