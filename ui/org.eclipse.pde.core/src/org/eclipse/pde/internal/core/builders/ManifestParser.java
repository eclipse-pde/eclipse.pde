package org.eclipse.pde.internal.core.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.xml.sax.helpers.*;
import org.apache.xerces.parsers.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

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
		PDEPlugin.logException(e);
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
