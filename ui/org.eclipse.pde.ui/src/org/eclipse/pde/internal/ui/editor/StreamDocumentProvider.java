package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public abstract class StreamDocumentProvider extends AbstractDocumentProvider {
	private IDocumentPartitioner partitioner;
	private String enc;

public StreamDocumentProvider(IDocumentPartitioner partitioner, String encoding) {
	this.partitioner = partitioner;
	this.enc = encoding;
}

protected IDocumentPartitioner getPartitioner() {
	return partitioner;
}

protected String getEncoding() {
	return enc;
}

protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
	return null;
}
protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean force) throws CoreException {}
protected void setDocumentContent(IDocument document, InputStream contentStream) {
	try {
		Reader in;
		if (enc==null)
		   in = new InputStreamReader(contentStream);
		else
		   in = new InputStreamReader(contentStream, enc);
		int chunkSize = contentStream.available();
		StringBuffer buffer = new StringBuffer(chunkSize);
		char[] readBuffer = new char[chunkSize];
		int n = in.read(readBuffer);
		while (n > 0) {
			buffer.append(readBuffer);
			n = in.read(readBuffer);
		}
		in.close();
		document.set(buffer.toString());

	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
}
	public long getSynchronizationStamp(Object element) {
		return 0;
	}

	public long getModificationStamp(Object element) {
		return 0;
	}
	
	public boolean isDeleted(Object element) {
		return false;
	}

	protected IDocument createEmptyDocument() {
		return new Document();
	}
}
