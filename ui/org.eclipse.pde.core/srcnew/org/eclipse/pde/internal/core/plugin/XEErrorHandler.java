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
package org.eclipse.pde.internal.core.plugin;

import org.apache.xerces.impl.xs.util.SimpleLocator;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;


public class XEErrorHandler implements XMLErrorHandler {
	
	protected class XMLProblem extends SourceRange implements IProblem {
		private String fCode, fMessage;
		private int fSeverity;
		
		public XMLProblem(String code, String message, int severity, int startLine, int startColumn, int endLine, int endColumn) {
			super(startLine, startColumn, endLine, endColumn);
			fCode= code;
			fMessage= message;
			fSeverity= severity;
		}
		
		/*
		 * @see org.eclipse.ui.examples.xmleditor2.model.IProblem#getCode()
		 */
		public String getCode() {
			return fCode;
		}
		
		/*
		 * @see org.eclipse.ui.examples.xmleditor2.model.IProblem#getMessage()
		 */
		public String getMessage() {
			return fMessage;
		}
		
		/*
		 * @see org.eclipse.ui.examples.xmleditor2.model.IProblem#isError()
		 */
		public boolean isError() {
			return fSeverity == SEVERTITY_ERROR || fSeverity == SEVERTITY_FATAL_ERROR;
		}
		
		/*
		 * @see org.eclipse.ui.examples.xmleditor2.model.IProblem#isWarning()
		 */
		public boolean isWarning() {
			return fSeverity == SEVERTITY_WARNING;
		}
		
	}
	
	private IProblemRequestor fProblemRequestor;
	private static final int SEVERTITY_WARNING= 0;
	private static final int SEVERTITY_ERROR= 1;
	private static final int SEVERTITY_FATAL_ERROR= 2;
	
	private int errorCount;
	private int fatalErrorCount;
	private int warningCount;

	/**
	 * Constructor XEErrorHandler.
	 * @param resourceProvider
	 */
	public XEErrorHandler(IProblemRequestor problemRequestor) {
		fProblemRequestor= problemRequestor;
	}

	public void beginReporting() {
		if (fProblemRequestor != null) {
			fProblemRequestor.beginReporting();
		}
	}
	
	public void endReporting() {
		if (fProblemRequestor != null) {
			fProblemRequestor.endReporting();
		}
	}

	protected IProblem createProblem(String domain, String key, XMLParseException exception, int severity) {
		int line= exception.getLineNumber();
		int column= exception.getColumnNumber();
		return new XMLProblem(key, exception.getMessage(), severity, line, column, line, column); //REVISIT: supply a *range*
	}

	public void notifyProblemRequestor(String domain, String key, XMLParseException exception, int severity) {
		if (fProblemRequestor != null) {
			IProblem problem= createProblem(domain, key, exception, severity);
			fProblemRequestor.acceptProblem(problem);
		}
	}

	/*
	 * @see org.apache.xerces.xni.parser.XMLErrorHandler#warning(java.lang.String, java.lang.String, org.apache.xerces.xni.parser.XMLParseException)
	 */
	public void warning(String domain, String key, XMLParseException exception) throws XNIException {
		warningCount++;
		notifyProblemRequestor(domain, key, exception, SEVERTITY_WARNING);
	}
	
	/*
	 * @see org.apache.xerces.xni.parser.XMLErrorHandler#error(java.lang.String, java.lang.String, org.apache.xerces.xni.parser.XMLParseException)
	 */
	public void error(String domain, String key, XMLParseException exception) throws XNIException {
		errorCount++;
		notifyProblemRequestor(domain, key, exception, SEVERTITY_ERROR);
	}

	/*
	 * @see org.apache.xerces.xni.parser.XMLErrorHandler#fatalError(java.lang.String, java.lang.String, org.apache.xerces.xni.parser.XMLParseException)
	 */
	public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
		fatalErrorCount++;
		notifyProblemRequestor(domain, key, exception, SEVERTITY_FATAL_ERROR);
		XMLLocator locator = new SimpleLocator(null, null, exception.getLineNumber(), exception.getColumnNumber());
		throw new XEParseException(locator, exception.getMessage(), key); //NOTE: this is required (see super.fatalError(...))
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
}
