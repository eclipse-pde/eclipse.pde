/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.HashMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class BundleTextChangeListener implements IModelTextChangeListener {

	private HashMap fOperationTable = new HashMap();
	private IDocument fDocument;
	
	public BundleTextChangeListener(IDocument document) {
		fDocument = document;
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null) {
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
                if (object instanceof PDEManifestElement)
                    object = ((PDEManifestElement)object).getHeader();
                else if (object instanceof PackageFriend)
                    object = ((PackageFriend)object).getHeader();
                
				if (object instanceof ManifestHeader) {
					ManifestHeader header = (ManifestHeader)object;
					TextEdit op = (TextEdit)fOperationTable.get(header);
					if (op != null) {
						fOperationTable.remove(header);
					}
					if (header.getValue() == null || header.getValue().trim().length() == 0) {
						deleteKey(header);						
					} else {
						modifyKey(header);
					}
				}
			}
		}
	}

	private void insertKey(IDocumentKey key) {
		int offset = fDocument.getLength();
		for (int i = fDocument.getNumberOfLines() - 1; i >= 0; i--) {
			try {
				if (fDocument.get(fDocument.getLineOffset(i), fDocument.getLineLength(i)).trim().length() > 0) {
					break;
				}
				offset = fDocument.getLineOffset(i);
			} catch (BadLocationException e) {
			}
		}
		InsertEdit op = new InsertEdit(offset, key.write()); 
		fOperationTable.put(key, op);
	}
	
	private void deleteKey(IDocumentKey key) {
		if (key.getOffset() > 0) {
			TextEdit op = new DeleteEdit(key.getOffset(), key.getLength());
			fOperationTable.put(key, op);
		}
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
		return (TextEdit[])fOperationTable.values().toArray(new TextEdit[fOperationTable.size()]);
	}

}
