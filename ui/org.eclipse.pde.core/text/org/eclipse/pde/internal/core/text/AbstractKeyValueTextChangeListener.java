/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public abstract class AbstractKeyValueTextChangeListener extends AbstractTextChangeListener {

	public AbstractKeyValueTextChangeListener(IDocument document) {
		super(document);
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
		result[0] = new InsertEdit(PropertiesUtil.getInsertOffset(fDocument), fSep);
		System.arraycopy(ops, 0, result, 1, ops.length);
		return result;
	}

	protected void insertKey(IDocumentKey key) {
		int offset = PropertiesUtil.getInsertOffset(fDocument);
		fOperationTable.put(key, new InsertEdit(offset, key.write()));
	}
	
	protected void deleteKey(IDocumentKey key) {
		if (key.getOffset() >= 0) 
			fOperationTable.put(key, new DeleteEdit(key.getOffset(), key.getLength()));
	}
	
	protected void modifyKey(IDocumentKey key) {		
		if (key.getOffset() == -1)
			insertKey(key);
		else
			fOperationTable.put(key, new ReplaceEdit(key.getOffset(), key.getLength(), key.write()));
	}

}
