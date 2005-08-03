/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.bundle.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class BundleInputContext extends UTF8InputContext {
	public static final String CONTEXT_ID = "bundle-context"; //$NON-NLS-1$
	
	private HashMap fOperationTable = new HashMap();
	/**
	 * @param editor
	 * @param input
	 */
	public BundleInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		BundleModel model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			model = new BundleModel(document, isReconciling);
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
                if (object instanceof PackageObject)
                    object = ((PackageObject)object).getHeader();
                else if (object instanceof PackageFriend)
                    object = ((PackageFriend)object).getHeader();
                
				if (object instanceof ManifestHeader) {
					ManifestHeader header = (ManifestHeader)object;
					TextEdit op = (TextEdit)fOperationTable.get(header);
					if (op != null) {
						fOperationTable.remove(header);
						ops.remove(op);
					}
					if (header.getValue() == null || header.getValue().trim().length() == 0) {
							deleteKey(header, ops);						
					} else {
						modifyKey(header, ops);
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getMoveOperations()
	 */
	protected TextEdit[] getMoveOperations() {
		return new TextEdit[0];
	}
	
	private void insertKey(IDocumentKey key, ArrayList ops) {
		IDocument doc = getDocumentProvider().getDocument(getInput());
		int offset = doc.getLength();
		for (int i = doc.getNumberOfLines() - 1; i >= 0; i--) {
			try {
				if (doc.get(doc.getLineOffset(i), doc.getLineLength(i)).trim().length() > 0) {
					break;
				}
				offset = doc.getLineOffset(i);
			} catch (BadLocationException e) {
			}
		}
		InsertEdit op = new InsertEdit(offset, key.write() + getLineDelimiter()); 
		fOperationTable.put(key, op);
		ops.add(op);
	}
	
	private void deleteKey(IDocumentKey key, ArrayList ops) {
		if (key.getOffset() > 0) {
			TextEdit op = new DeleteEdit(key.getOffset(), key.getLength());
			fOperationTable.put(key, op);
			ops.add(op);
		}
	}
	
	private void modifyKey(IDocumentKey key, ArrayList ops) {		
		if (key.getOffset() == -1) {
			insertKey(key, ops);
		} else {
			TextEdit op = new ReplaceEdit(key.getOffset(), key.getLength(), key
					.write()
					+ getLineDelimiter()); 
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
		return "___bundle_partition"; //$NON-NLS-1$
	}
}
