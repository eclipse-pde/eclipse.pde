package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.xml.sax.*;

public class XMLErrorHandler implements ErrorHandler {
	private int errorCount;
	private int fatalErrorCount;
	private int warningCount;

public XMLErrorHandler() {
}
public void error(SAXParseException exception) throws SAXException {
	errorCount++;
}
public void fatalError(SAXParseException exception) throws SAXException {
	fatalErrorCount++;
}
public int getErrorCount() {
	return errorCount;
}
public int getFatalErrorCount() {
	return fatalErrorCount;
}
public int getWarningCount() {
	return warningCount;
}
public void reset() {
	errorCount = 0;
	fatalErrorCount = 0;
	warningCount = 0;
}
public void warning(SAXParseException exception) throws SAXException {
	warningCount++;
}
}
