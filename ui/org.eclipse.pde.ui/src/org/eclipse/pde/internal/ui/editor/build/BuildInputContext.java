/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.build.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class BuildInputContext extends InputContext {
	public static final String CONTEXT_ID = "build-context"; //$NON-NLS-1$
	
	private HashMap fOperationTable = new HashMap();

	public BuildInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getCharSet()
	 */
	protected String getDefaultCharset() {
		return "ISO-8859-1"; //$NON-NLS-1$
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
			} else {
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
		String preTermination = ""; //$NON-NLS-1$
		if (doc.getNumberOfLines() > 0) {
			try {
				if (doc.getLineDelimiter(doc.getNumberOfLines() - 1) == null
						&& doc.getLineLength(doc.getNumberOfLines() - 1) > 0) {
					preTermination = System.getProperty("line.separator"); //$NON-NLS-1$
				}
			} catch (BadLocationException ble) {
			}
		}
		InsertEdit op = new InsertEdit(doc.getLength(), preTermination
				+ key.write());
		fOperationTable.put(key, op);
		ops.add(op);
	}
	
	private void deleteKey(IDocumentKey key, ArrayList ops) {
		if (key.getOffset() >= 0) {
			TextEdit op = new DeleteEdit(key.getOffset(), key.getLength());
			fOperationTable.put(key, op);
			ops.add(op);
		}
	}
	
	private void modifyKey(IDocumentKey key, ArrayList ops) {		
		if (key.getOffset() == -1) {
			insertKey(key, ops);
		} else {
			TextEdit op = new ReplaceEdit(key.getOffset(), key.getLength(), key.write());
			fOperationTable.put(key, op);
			ops.add(op);
		}	
	}
	public void doRevert() {
		fEditOperations.clear();
		fOperationTable.clear();
		AbstractEditingModel model = (AbstractEditingModel)getModel();
		model.reconciled(model.getDocument());
	}

	protected String getPartitionName() {
		return "___build_partition"; //$NON-NLS-1$
	}	
}
