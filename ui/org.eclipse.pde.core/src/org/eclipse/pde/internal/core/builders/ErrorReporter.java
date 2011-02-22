/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fabio Mancinelli <fm@fabiomancinelli.org> - bug 201304
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.PDECore;

public abstract class ErrorReporter {

	protected static final String[] BOOLEAN_VALUES = new String[] {"true", "false"}; //$NON-NLS-1$ //$NON-NLS-2$

	private int fErrorCount;
	protected IFile fFile;
	protected IProject fProject;
	private PDEMarkerFactory fMarkerFactory;

	public ErrorReporter(IFile file) {
		fErrorCount = 0;
		fFile = file;
		if (fFile != null) {
			fProject = fFile.getProject();
		}
	}

	protected IMarker addMarker(String message, int lineNumber, int severity, int problemID, String category) {
		try {
			IMarker marker = getMarkerFactory().createMarker(fFile, problemID, category);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1)
				lineNumber = 1;
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			if (severity == IMarker.SEVERITY_ERROR) {
				fErrorCount += 1;
			}
			return marker;
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return null;
	}

	protected IDocument createDocument(IFile file) {
		if (!file.exists()) {
			return null;
		}
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		if (manager == null) {
			return null;
		}
		try {
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, null);
			ITextFileBuffer textBuf = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);
			IDocument document = textBuf.getDocument();
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, null);
			return document;
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return null;
	}

	public int getErrorCount() {
		return fErrorCount;
	}

	private PDEMarkerFactory getMarkerFactory() {
		if (fMarkerFactory == null)
			fMarkerFactory = new PDEMarkerFactory();
		return fMarkerFactory;
	}

	private void removeFileMarkers() {
		try {
			fFile.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			fFile.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}

	/**
	 * Return a new marker with the provided attributes.  May return <code>null</code> if no marker should be created because of severity settings.
	 * 
	 * @param message
	 * @param line
	 * @param severity
	 * @param problemID
	 * @param category
	 * @return a new marker or <code>null</code>
	 */
	public IMarker report(String message, int line, int severity, int problemID, String category) {
		if (severity == CompilerFlags.ERROR)
			return addMarker(message, line, IMarker.SEVERITY_ERROR, problemID, category);
		else if (severity == CompilerFlags.WARNING)
			return addMarker(message, line, IMarker.SEVERITY_WARNING, problemID, category);
		return null;
	}

	public IMarker report(String message, int line, int severity, String category) {
		return report(message, line, severity, PDEMarkerFactory.NO_RESOLUTION, category);
	}

	protected IMarker report(String message, int line, String compilerFlag, int problemID, String category) {
		int severity = CompilerFlags.getFlag(fProject, compilerFlag);
		if (severity != CompilerFlags.IGNORE) {
			return report(message, line, severity, problemID, category);
		}
		return null;
	}

	protected void report(String message, int line, String compilerFlag, String category) {
		report(message, line, compilerFlag, PDEMarkerFactory.NO_RESOLUTION, category);
	}

	public void validateContent(IProgressMonitor monitor) {
		removeFileMarkers();
		validate(monitor);
	}

	protected abstract void validate(IProgressMonitor monitor);
}
