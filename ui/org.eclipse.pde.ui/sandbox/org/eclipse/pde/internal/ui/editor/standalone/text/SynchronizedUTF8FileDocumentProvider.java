/*
 * Created on Sep 27, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.text;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.texteditor.*;

/**
 * @author melhem
 */
public class SynchronizedUTF8FileDocumentProvider extends FileDocumentProvider {
	public IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new DefaultPartitioner(
					new XMLPartitionScanner(),
					new String[] {
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT });
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.StorageDocumentProvider#createEmptyDocument()
	 */
	protected IDocument createEmptyDocument() {
		return new PartiallySynchronizedDocument();
	}

	protected void setDocumentContent(
		IDocument document,
		InputStream contentStream,
		String encoding)
		throws CoreException {

		Reader in = null;

		try {

			in = new InputStreamReader(new BufferedInputStream(contentStream), "UTF8");
			StringBuffer buffer = new StringBuffer();
			char[] readBuffer = new char[2048];
			int n = in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n = in.read(readBuffer);
			}

			document.set(buffer.toString());

		} catch (IOException x) {
			PDEPlugin.logException(x);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
				}
			}
		}
	}
	protected void doSaveDocument(
		IProgressMonitor monitor,
		Object element,
		IDocument document,
		boolean overwrite)
		throws CoreException {
		if (element instanceof IFileEditorInput) {

			IFileEditorInput input = (IFileEditorInput) element;
			InputStream stream = null;
			try {
				stream = new ByteArrayInputStream(document.get().getBytes("UTF8"));
				IFile file = input.getFile();
				if (file.exists()) {
					FileInfo info = (FileInfo) getElementInfo(element);
					if (info != null && !overwrite)
						checkSynchronizationState(info.fModificationStamp, file);
					file.setContents(stream, overwrite, true, monitor);
					if (info != null) {
						ResourceMarkerAnnotationModel model =
							(ResourceMarkerAnnotationModel) info.fModel;
						model.updateMarkers(info.fDocument);
						info.fModificationStamp = computeModificationStamp(file);
					}
				} else {
					try {
						monitor.beginTask("Saving", 2000);
						ContainerGenerator generator =
							new ContainerGenerator(file.getParent().getFullPath());
						generator.generateContainer(
							new SubProgressMonitor(monitor, 1000));
						file.create(stream, false, new SubProgressMonitor(monitor, 1000));
					} finally {
						monitor.done();
					}
				}
			} catch (UnsupportedEncodingException e) {
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		} else {
			super.doSaveDocument(monitor, element, document, overwrite);
		}
	}

}
