package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SystemFileDocumentProvider extends StreamDocumentProvider {

	public SystemFileDocumentProvider(IDocumentPartitioner partitioner) {
		this(partitioner, null);
	}

	public SystemFileDocumentProvider(
		IDocumentPartitioner partitioner,
		String encoding) {
		super(partitioner, encoding);
	}
	/*
	 * @see AbstractDocumentProvider#createAnnotationModel(Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		if (element instanceof SystemFileEditorInput) {
			SystemFileEditorInput input= (SystemFileEditorInput) element;
			File file = (File)input.getAdapter(File.class);
			if (file!=null) {
				return new SystemFileMarkerAnnotationModel(file);
			}
		}
		return super.createAnnotationModel(element);
	}

	protected IDocument createDocument(Object element) throws CoreException {
		if (element instanceof SystemFileEditorInput) {
			Document document = new Document();
			IDocumentPartitioner part = getPartitioner();
			if (part != null) {
				part.connect(document);
				document.setDocumentPartitioner(part);
			}
			File file =
				(File) ((SystemFileEditorInput) element).getAdapter(File.class);
			setDocumentContent(document, file);
			return document;
		}
		return null;
	}
	protected void doSaveDocument(
		IProgressMonitor monitor,
		Object element,
		IDocument document,
		boolean force)
		throws CoreException {
	}
	protected void setDocumentContent(IDocument document, File file) {
		try {
			InputStream contentStream = new FileInputStream(file);
			setDocumentContent(document, contentStream);
			contentStream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

}