package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.*;
import java.io.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.SubProgressMonitor;

public abstract class PDEMultiPageXMLEditor extends PDEMultiPageEditor {
	
class UTF8FileDocumentProvider extends FileDocumentProvider {
	public IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner = createDocumentPartitioner();
			if (partitioner != null) {
				partitioner.connect(document);
				document.setDocumentPartitioner(partitioner);
			}
		}
		return document;
	}
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
		if (element instanceof IFileEditorInput) {
			
			IFileEditorInput input= (IFileEditorInput) element;
			InputStream stream = null;
			try {
			   stream= new ByteArrayInputStream(document.get().getBytes("UTF8"));
			}
			catch (UnsupportedEncodingException e) {
			}
			
			IFile file= input.getFile();
									
			if (file.exists()) {				
				
				FileInfo info= (FileInfo) getElementInfo(element);
				
				if (info != null && !overwrite)
					checkSynchronizationState(info.fModificationStamp, file);
				
				file.setContents(stream, overwrite, true, monitor);
				
				if (info != null) {
										
					ResourceMarkerAnnotationModel model= (ResourceMarkerAnnotationModel) info.fModel;
					model.updateMarkers(info.fDocument);
					
					info.fModificationStamp= computeModificationStamp(file);
				}
				
			} else {
				try {
					//monitor.beginTask(TextEditorMessages.getString("FileDocumentProvider.task.saving"), 2000); //$NON-NLS-1$
					monitor.beginTask("Saving", 2000);
					ContainerGenerator generator = new ContainerGenerator(file.getParent().getFullPath());
					generator.generateContainer(new SubProgressMonitor(monitor, 1000));
					file.create(stream, false, new SubProgressMonitor(monitor, 1000));
				}
				finally {
					monitor.done();
				}
			}
		} else {
			super.doSaveDocument(monitor, element, document, overwrite);
		}
	}
}

public PDEMultiPageXMLEditor() {
	super();
}
protected IDocumentPartitioner createDocumentPartitioner() {
	RuleBasedPartitioner partitioner =
		new RuleBasedPartitioner(
			new PDEPartitionScanner(),
			new String[] { PDEPartitionScanner.XML_TAG, PDEPartitionScanner.XML_COMMENT });
	return partitioner;
}

protected IDocumentProvider createDocumentProvider(Object input) {
	IDocumentProvider documentProvider = null;
	if (input instanceof IFile)
		documentProvider = new UTF8FileDocumentProvider();
	else 
		if (input instanceof File)
			documentProvider = new SystemFileDocumentProvider(createDocumentPartitioner(), "UTF8");
   	return documentProvider;
}
}
