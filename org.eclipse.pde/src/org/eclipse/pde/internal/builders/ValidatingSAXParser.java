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
import java.net.*;

import javax.xml.parsers.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.xml.sax.*;

/**
 * @author Wassim Melhem
 */
public class ValidatingSAXParser {
	
	private static SAXParserFactory fFactory;
	
	public static void parse(IFile file, PluginErrorReporter reporter, boolean useSystemId) {
		try {
			if (!useSystemId) {
				parse(file, reporter);
				return;
			}
			InputSource source = new InputSource(file.getContents());
			URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
			source.setSystemId(dtdLocation.toString());
			getParser().parse(source, reporter);
		} catch (SAXException e) {
		} catch (ParserConfigurationException e) {
		} catch (IOException e) {
		} catch (CoreException e) {
		}
	}
	
	public static void parse(IFile file, PluginErrorReporter reporter) {
		try {
			parse(file.getContents(), reporter);
		} catch (CoreException e) {
		}
	}
	
	public static void parse(InputStream is, PluginErrorReporter reporter) {
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
