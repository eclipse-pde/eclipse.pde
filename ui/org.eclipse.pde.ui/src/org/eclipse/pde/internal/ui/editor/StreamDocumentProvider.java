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
package org.eclipse.pde.internal.ui.editor;

import java.io.*;

import org.eclipse.jface.operation.*;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		//TODO figure out what this method does
		return null;
	}
}
