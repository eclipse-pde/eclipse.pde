/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.build.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class BuildInputContext extends InputContext {
	public static final String CONTEXT_ID = "build-context";
	
	private HashMap fOperationTable = new HashMap();

	public BuildInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getCharSet()
	 */
	protected String getDefaultCharset() {
		return "ISO-8859-1";
	}

	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		BuildModel model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			model = new BuildModel(document, isReconciling);
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput)input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(file.getCharset());
			} else if (input instanceof SystemFileEditorInput){
				File file = (File)((SystemFileEditorInput)input).getAdapter(File.class);
				model.setInstallLocation(file.getParent());
				model.setCharset(getDefaultCharset());
			}
			model.load();
		}
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null) {
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				IDocumentKey key = (IDocumentKey)object;
				TextEdit op = (TextEdit)fOperationTable.get(key);
				if (op != null) {
					fOperationTable.remove(key);
					ops.remove(op);
				}
				switch (event.getChangeType()) {
					case IModelChangedEvent.REMOVE :
						deleteKey(key, ops);
						break;
					case IModelChangedEvent.INSERT :
						insertKey(key, ops);
						break;
					case IModelChangedEvent.CHANGE :
						modifyKey(key, ops);
					default:
						break;
				}
			}
		}
	}
	
	private void insertKey(IDocumentKey key, ArrayList ops) {
		IDocument doc = getDocumentProvider().getDocument(getInput());
		InsertEdit op = new InsertEdit(doc.getLength(), key.write());
		fOperationTable.put(key, op);
		ops.add(op);
	}
	
	private void deleteKey(IDocumentKey key, ArrayList ops) {
		if (key.getOffset() > 0) {
			try {
				TextEdit op = new DeleteEdit(key.getOffset(), getLength(key));
				fOperationTable.put(key, op);
				ops.add(op);
			} catch (BadLocationException e) {
			}
		}
	}
	
	private void modifyKey(IDocumentKey key, ArrayList ops) {		
		if (key.getOffset() == -1) {
			insertKey(key, ops);
		} else {
			try {
				TextEdit op = new ReplaceEdit(key.getOffset(), getLength(key), key.write());
				fOperationTable.put(key, op);
				ops.add(op);
			} catch (BadLocationException e) {			
			}
		}	
	}
	
	private int getLength(IDocumentKey key) throws BadLocationException {
		IDocument doc = getDocumentProvider().getDocument(getInput());
		int endline = doc.getLineOfOffset(key.getOffset()) + key.getLineSpan();
		return doc.getLineOffset(endline) + doc.getLineLength(endline) - key.getOffset();
	}	

}
