package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.editor.text.*;
import java.io.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.texteditor.*;
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
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

}