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

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.*;

public class ManifestParser {
	private SAXParser parser;
	private PluginErrorReporter reporter;

public ManifestParser(PluginErrorReporter reporter) {
	this.reporter = reporter;
	parser = new SAXParser();
	parser.setErrorHandler(reporter);
}

public void parse(IFile file) {
	InputStream source = null;
	try {
		source = file.getContents();
		InputSource inputSource = new InputSource(source);
		parser.parse(inputSource);
	} catch (CoreException e) {
	} catch (SAXException e) {
	} catch (IOException e) {
		PDECore.logException(e);
	} finally {
		if (source != null) {
			try {
				source.close();
			} catch (IOException e) {
			}
		}
	}
}
}
