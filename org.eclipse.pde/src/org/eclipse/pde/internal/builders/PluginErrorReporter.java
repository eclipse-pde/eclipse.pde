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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class PluginErrorReporter extends DefaultHandler {
	private IFile file;
	private int errorCount;
	private IMarkerFactory markerFactory;
	private DefaultMarkerFactory defaultMarkerFactory;
	
	class DefaultMarkerFactory implements IMarkerFactory {
		public IMarker createMarker(IFile file) throws CoreException {
			return file.createMarker(IMarker.PROBLEM);
		}
	}
	
	public PluginErrorReporter() {
	}

	public PluginErrorReporter(IFile file) {
		errorCount = 0;
		this.file = file;
		if (file!=null) {
			removeFileMarkers(); 
			defaultMarkerFactory = new DefaultMarkerFactory();
			markerFactory = defaultMarkerFactory;
		}
	}
	
	public IFile getFile() {
		return file;
	}
	
	public void setMarkerFactory(IMarkerFactory factory) {
		if (factory!=null) markerFactory = factory;
		else
			markerFactory = defaultMarkerFactory;
	}

	private void addMarker(
		String message,
		int lineNumber,
		int severity,
		boolean fatal) {
		try {
			IMarker marker = markerFactory.createMarker(file);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber != -1)
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}

	private void addMarker(SAXParseException e, int severity, boolean fatal) {
		addMarker(e.getMessage(), e.getLineNumber(), severity, fatal);
	}

	public void error(SAXParseException exception) throws SAXException {
		if (file!=null) addMarker(exception, IMarker.SEVERITY_ERROR, false);
		errorCount++;
	}
	public void fatalError(SAXParseException exception) throws SAXException {
		if (file!=null) addMarker(exception, IMarker.SEVERITY_ERROR, true);
		errorCount++;
	}
	public int getErrorCount() {
		return errorCount;
	}
	private void removeFileMarkers() {
		try {
			file.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			file.deleteMarkers(SchemaMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}
	public void reportError(String message) {
		reportError(message, -1);
	}
	
	public void report(String message, int line, int severity) {
		if (severity==CompilerFlags.ERROR)
			reportError(message, line);
		else if (severity==CompilerFlags.WARNING)
			reportWarning(message, line);
	}

	public void reportError(String message, int line) {
		errorCount++;
		if (file!=null)
			addMarker(message, line, IMarker.SEVERITY_ERROR, false);
	}

	public void reportWarning(String message) {
		reportWarning(message, -1);
	}

	public void reportWarning(String message, int line) {
		addMarker(message, line, IMarker.SEVERITY_WARNING, false);
	}

	public void warning(SAXParseException exception) throws SAXException {
		addMarker(exception, IMarker.SEVERITY_WARNING, false);
	}
}
