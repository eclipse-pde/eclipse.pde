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

import java.io.*;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Wassim Melhem
 */
public class ValidatingSAXParser {
	
	private static SAXParserFactory fFactory;
	private static SAXParser fParser;
	
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
	
	private static SAXParserFactory getParserFactory() {
		if (fFactory == null) {
			fFactory = SAXParserFactory.newInstance();
		}
		return fFactory;
	}
	
	private static SAXParser getParser()
		throws ParserConfigurationException, SAXException {
		if (fParser == null) {
			fParser = getParserFactory().newSAXParser();
		}
		return fParser;
	}
}
