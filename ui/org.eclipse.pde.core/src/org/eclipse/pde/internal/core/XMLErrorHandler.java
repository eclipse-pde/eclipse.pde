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
package org.eclipse.pde.internal.core;

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
