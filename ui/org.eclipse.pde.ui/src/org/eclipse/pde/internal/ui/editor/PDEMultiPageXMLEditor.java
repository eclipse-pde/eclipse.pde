package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.PDEPartitionScanner;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.*;

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
		protected void setDocumentContent(
			IDocument document,
			InputStream contentStream,
			String encoding)
			throws CoreException {

			Reader in = null;

			try {

				in =
					new InputStreamReader(
						new BufferedInputStream(contentStream),
						"UTF8");
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
					stream =
						new ByteArrayInputStream(
							document.get().getBytes("UTF8"));
				} catch (UnsupportedEncodingException e) {
				}

				IFile file = input.getFile();

				if (file.exists()) {

					FileInfo info = (FileInfo) getElementInfo(element);

					if (info != null && !overwrite)
						checkSynchronizationState(
							info.fModificationStamp,
							file);

					file.setContents(stream, overwrite, true, monitor);

					if (info != null) {

						ResourceMarkerAnnotationModel model =
							(ResourceMarkerAnnotationModel) info.fModel;
						model.updateMarkers(info.fDocument);

						info.fModificationStamp =
							computeModificationStamp(file);
					}

				} else {
					try {
						//monitor.beginTask(TextEditorMessages.getString("FileDocumentProvider.task.saving"), 2000); //$NON-NLS-1$
						monitor.beginTask("Saving", 2000);
						ContainerGenerator generator =
							new ContainerGenerator(
								file.getParent().getFullPath());
						generator.generateContainer(
							new SubProgressMonitor(monitor, 1000));
						file.create(
							stream,
							false,
							new SubProgressMonitor(monitor, 1000));
					} finally {
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
		DefaultPartitioner partitioner =
			new DefaultPartitioner(
				new PDEPartitionScanner(),
				new String[] {
					PDEPartitionScanner.XML_TAG,
					PDEPartitionScanner.XML_COMMENT });
		return partitioner;
	}

	protected IDocumentProvider createDocumentProvider(Object input) {
		IDocumentProvider documentProvider = null;
		if (input instanceof IFile)
			documentProvider = new UTF8FileDocumentProvider();
		else if (input instanceof File)
			documentProvider =
				new SystemFileDocumentProvider(
					createDocumentPartitioner(),
					"UTF8");
		else if (input instanceof IStorage)
			documentProvider =
				new StorageDocumentProvider(
					createDocumentPartitioner(),
					"UTF8");
		return documentProvider;
	}
}