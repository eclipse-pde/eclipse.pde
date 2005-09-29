package org.eclipse.pde.internal.ui.nls;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class ExternalizeStringsOperation extends WorkspaceModifyOperation {

	private Object[] fChangeFiles;
	
	public ExternalizeStringsOperation(Object[] changeFiles) {
		fChangeFiles = changeFiles;
	}
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		for (int i = 0; i < fChangeFiles.length; i++) {
			if (fChangeFiles[i] instanceof ModelChangeFile) {
				ModelChangeFile changeFile = (ModelChangeFile)fChangeFiles[i];
				ModelChange change = changeFile.getModel();
				IFile pFile = change.getPropertiesFile();
				if (!pFile.exists()) {
					IPluginModelBase model = change.getParentModel();
					String propertiesFileComment = "# properties file for "  //$NON-NLS-1$
						+ model.getUnderlyingResource().getProject().getName();
					ByteArrayInputStream pStream = new ByteArrayInputStream(propertiesFileComment.getBytes());
					pFile.create(pStream, true, monitor);
					if (!change.localizationSet()) {
						addBundleLocalization(pFile.getProject(), change.getBundleLocalization(), monitor);
					}
				}
				
				ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
				try {
					pManager.connect(pFile.getFullPath(), monitor);
					ITextFileBuffer pBuffer = pManager.getTextFileBuffer(pFile.getFullPath());
					IDocument pDoc = pBuffer.getDocument();
					MultiTextEdit pEdit = new MultiTextEdit();
					
					doReplace(changeFile, pDoc, pEdit, monitor);
					
					pEdit.apply(pDoc);
					pBuffer.commit(monitor, true);
					
				} catch (MalformedTreeException e) {
				} catch (BadLocationException e) {
				} finally {
					pManager.disconnect(pFile.getFullPath(), monitor);
				}
			}
		}
	}
	private void doReplace(ModelChangeFile changeFile, IDocument pDoc, MultiTextEdit pEdit, IProgressMonitor monitor) throws CoreException {
		IFile uFile = changeFile.getFile();
		ITextFileBufferManager uManager = FileBuffers.getTextFileBufferManager();
		try {
			uManager.connect(uFile.getFullPath(), monitor);
			ITextFileBuffer uBuffer = uManager.getTextFileBuffer(uFile.getFullPath());
			IDocument uDoc = uBuffer.getDocument();
			MultiTextEdit uEdit = new MultiTextEdit();
			
			String nl = TextUtilities.getDefaultLineDelimiter(pDoc);
			Iterator iter = changeFile.getChanges().iterator();
			
			while (iter.hasNext()) {
				ModelChangeElement changeElement = (ModelChangeElement)iter.next();
				if (changeElement.isExternalized()) {
					uEdit.addChild(new ReplaceEdit(changeElement.getOffset(),
							changeElement.getLength(), 
							changeElement.getExternKey()));
					pEdit.addChild(new InsertEdit(pDoc.getLength(), 
							nl + changeElement.getKey() + " = " +  //$NON-NLS-1$
							StringHelper.preparePropertiesString(changeElement.getValue(), nl.toCharArray())));
				}
			}
			uEdit.apply(uDoc);
			uBuffer.commit(monitor, true);
			
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			uManager.disconnect(uFile.getFullPath(), monitor);
		}
 	}
	
	private void addBundleLocalization(IProject project, String localization, IProgressMonitor monitor) throws CoreException {
		IFile mFile = project.getFile(GetNonExternalizedStringsOperation.MANIFEST_LOCATION);
		if (!mFile.exists()) return;
		ITextFileBufferManager mManager = FileBuffers.getTextFileBufferManager();
		try {
			mManager.connect(mFile.getFullPath(), monitor);
			ITextFileBuffer mBuffer = mManager.getTextFileBuffer(mFile.getFullPath());
			IDocument mDoc = mBuffer.getDocument();
			
			String nl = TextUtilities.getDefaultLineDelimiter(mDoc);

			TextEdit mEdit = checkTrailingNewline(mDoc, nl);
			if (mEdit != null)
				mEdit.apply(mDoc);
			
			mEdit = new InsertEdit(mDoc.getLength(), 
					Constants.BUNDLE_LOCALIZATION + ": " + localization + nl); //$NON-NLS-1$
			mEdit.apply(mDoc);
			mBuffer.commit(monitor, true);
			
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			mManager.disconnect(mFile.getFullPath(), monitor);
		}
	}
	
	private TextEdit checkTrailingNewline(IDocument document, String ld) {
		try {
			int len = ld.length();
			if (!document.get(document.getLength() - len, len).equals(ld)) {
				return new InsertEdit(document.getLength(), ld);
			}
		} catch (BadLocationException e) {
		}
		return null;
	}
 }
