package org.eclipse.pde.internal.core.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
