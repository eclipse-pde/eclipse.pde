/*
 * Created on Sep 21, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.ui.*;

/**
 * @author melhem
 */
public class StandaloneXMLEditor extends AbstractXMLEditor {
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		try {
			createModel(getInputObject(input));
		} catch (Exception e) {
		}
	}
	
	private Object getInputObject(IEditorInput input) throws Exception {
		Object inputObject = null;
		if (input instanceof SystemFileEditorInput) {
			inputObject = input.getAdapter(File.class);
		} else if (input instanceof IFileEditorInput) {
			inputObject = input.getAdapter(IFile.class);
		} else if (input instanceof IStorageEditorInput) {
			inputObject = ((IStorageEditorInput) input).getStorage();
		}
		return inputObject;		
	}
	
	protected void createModel(Object input) throws Exception {
		fModel = new DocumentModel();
		if (input instanceof IFile) {
			fModel.load(((IFile)input).getContents());
		} else if (input instanceof IStorage) {
			fModel.load(((IStorage) input).getContents());
		} else if (input instanceof File) {
			fModel.load(new FileInputStream((File)input));
		}
	}

}
