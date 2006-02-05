/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.build;

import java.util.HashMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class PropertiesTextChangeListener implements IModelTextChangeListener {

	private HashMap fOperationTable = new HashMap();
	private IDocument fDocument;

	public PropertiesTextChangeListener(IDocument document) {
		fDocument = document;
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			IDocumentKey key = (IDocumentKey)object;
			fOperationTable.remove(key);
			switch (event.getChangeType()) {
				case IModelChangedEvent.REMOVE :
					deleteKey(key);
					break;
				default :
					modifyKey(key);
			}
		}
	}
	
	private void insertKey(IDocumentKey key) {
		int offset = PropertiesUtil.getInsertOffset(fDocument);
		fOperationTable.put(key, new InsertEdit(offset, key.write()));
	}
	
	private void deleteKey(IDocumentKey key) {
		if (key.getOffset() >= 0)
			fOperationTable.put(key, new DeleteEdit(key.getOffset(), key.getLength()));
	}
	
	private void modifyKey(IDocumentKey key) {		
		if (key.getOffset() == -1) {
			insertKey(key);
		} else {
			TextEdit op = new ReplaceEdit(key.getOffset(), key.getLength(), key.write());
			fOperationTable.put(key, op);
		}	
	}

	public TextEdit[] getTextOperations() {
		if (fOperationTable.size() == 0)
			return new TextEdit[0];
		
		TextEdit[] ops = (TextEdit[])fOperationTable.values().toArray(new TextEdit[fOperationTable.size()]);
		try {
			if (!PropertiesUtil.isNewlineNeeded(fDocument))
				return ops;
		} catch (BadLocationException e) {
		}
		
		TextEdit[] result = new TextEdit[fOperationTable.size() + 1];
		result[0] = new InsertEdit(PropertiesUtil.getInsertOffset(fDocument), TextUtilities.getDefaultLineDelimiter(fDocument));
		System.arraycopy(ops, 0, result, 1, ops.length);
		return result;
	}


}
