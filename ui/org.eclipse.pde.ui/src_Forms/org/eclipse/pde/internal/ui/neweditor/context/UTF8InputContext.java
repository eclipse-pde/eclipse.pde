/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.context;
import java.io.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class UTF8InputContext extends InputContext {
	protected class UTF8FileDocumentProvider extends FileDocumentProvider {
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
		protected void setDocumentContent(IDocument document,
				InputStream contentStream, String encoding) throws CoreException {
			Reader in = null;
			try {
				in = new InputStreamReader(new BufferedInputStream(contentStream),
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
		protected void doSaveDocument(IProgressMonitor monitor, Object element,
				IDocument document, boolean overwrite) throws CoreException {
			if (element instanceof IFileEditorInput) {
				IFileEditorInput input = (IFileEditorInput) element;
				InputStream stream = null;
				try {
					stream = new ByteArrayInputStream(document.get().getBytes(
							"UTF8"));
					IFile file = input.getFile();
					if (file.exists()) {
						FileInfo info = (FileInfo) getElementInfo(element);
						if (info != null && !overwrite)
							checkSynchronizationState(info.fModificationStamp, file);
						file.setContents(stream, overwrite, true, monitor);
						if (info != null) {
							ResourceMarkerAnnotationModel model = (ResourceMarkerAnnotationModel) info.fModel;
							model.updateMarkers(info.fDocument);
							info.fModificationStamp = computeModificationStamp(file);
						}
					} else {
						try {
							//monitor.beginTask(TextEditorMessages.getString("FileDocumentProvider.task.saving"),
							// 2000); //$NON-NLS-1$
							monitor.beginTask("Saving", 2000);
							ContainerGenerator generator = new ContainerGenerator(
									file.getParent().getFullPath());
							generator.generateContainer(new SubProgressMonitor(
									monitor, 1000));
							file.create(stream, false, new SubProgressMonitor(
									monitor, 1000));
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
	/**
	 * @param editor
	 * @param input
	 */
	public UTF8InputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createDocumentProvider(org.eclipse.ui.IEditorInput)
	 */
	protected IDocumentProvider createDocumentProvider(IEditorInput input) {
		IDocumentProvider documentProvider = null;
		if (input instanceof IFileEditorInput)
			documentProvider = new UTF8FileDocumentProvider();
		else if (input instanceof SystemFileEditorInput)
			documentProvider = new SystemFileDocumentProvider(
					createDocumentPartitioner(), "UTF8");
		else if (input instanceof IStorageEditorInput)
			documentProvider = new StorageDocumentProvider(
					createDocumentPartitioner(), "UTF8");
		return documentProvider;
	}

	protected IDocumentPartitioner createDocumentPartitioner() {
		return null;
	}
}