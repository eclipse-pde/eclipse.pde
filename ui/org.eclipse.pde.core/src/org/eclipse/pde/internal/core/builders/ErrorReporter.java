package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.PDECore;

public abstract class ErrorReporter {
	
	protected static final String[] BOOLEAN_VALUES = 
		new String[] { "true", "false" };  //$NON-NLS-1$ //$NON-NLS-2$

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

	protected IMarker addMarker(String message, int lineNumber, int severity,
			int problemID) {
		try {
			IMarker marker = getMarkerFactory().createMarker(fFile, problemID);
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
			manager.connect(file.getFullPath(), null);
			ITextFileBuffer textBuf = manager.getTextFileBuffer(file
					.getFullPath());
			IDocument document = textBuf.getDocument();
			manager.disconnect(file.getFullPath(), null);
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

	public IMarker report(String message, int line, int severity, int problemID) {
		if (severity == CompilerFlags.ERROR)
			return addMarker(message, line, IMarker.SEVERITY_ERROR, problemID);
		else if (severity == CompilerFlags.WARNING)
			return addMarker(message, line, IMarker.SEVERITY_WARNING, problemID);
		return null;
	}

	public IMarker report(String message, int line, int severity) {
		return report(message, line, severity, PDEMarkerFactory.NO_RESOLUTION);
	}

	protected IMarker report(String message, int line, String compilerFlag,
			int problemID) {
		int severity = CompilerFlags.getFlag(fProject, compilerFlag);
		if (severity != CompilerFlags.IGNORE) {
			return report(message, line, severity, problemID);
		}
		return null;
	}

	protected void report(String message, int line, String compilerFlag) {
		report(message, line, compilerFlag, PDEMarkerFactory.NO_RESOLUTION);
	}

	public void validateContent(IProgressMonitor monitor) {
		removeFileMarkers();
		validate(monitor);
	}
	
	protected abstract void validate(IProgressMonitor monitor);
}
