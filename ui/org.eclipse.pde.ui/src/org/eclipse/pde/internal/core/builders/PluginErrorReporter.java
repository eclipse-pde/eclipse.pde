package org.eclipse.pde.internal.core.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.*;

public class PluginErrorReporter implements ErrorHandler {
	private IFile file;
	private int errorCount;

public PluginErrorReporter(IFile file) {
	this.file = file;
	removeFileMarkers();
	errorCount = 0;
}
private void addMarker(
	String message,
	int lineNumber,
	int severity,
	boolean fatal) {
	try {
		IMarker marker = file.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		if (lineNumber != -1)
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}

private void addMarker(SAXParseException e, int severity, boolean fatal) {
	addMarker(e.getMessage(), e.getLineNumber(), severity, fatal);
}

public void error(SAXParseException exception) throws SAXException {
	addMarker(exception, IMarker.SEVERITY_ERROR, false);
	errorCount++;
}
public void fatalError(SAXParseException exception) throws SAXException {
	addMarker(exception, IMarker.SEVERITY_ERROR, true);
	errorCount++;
}
public int getErrorCount() {
	return errorCount;
}
private void removeFileMarkers() {
	try {
		file.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public void reportError(String message) {
	reportError(message, -1);
}

public void reportError(String message, int line) {
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
