/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.util.HashMap;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public abstract class AbstractKeyValueTextChangeListener extends AbstractTextChangeListener {

	protected HashMap<TextEdit, String> fReadableNames = null;

	public AbstractKeyValueTextChangeListener(IDocument document, boolean generateReadableNames) {
		super(document);
		if (generateReadableNames) {
			fReadableNames = new HashMap<>();
		}
	}

	@Override
	public TextEdit[] getTextOperations() {
		if (fOperationTable.isEmpty()) {
			return new TextEdit[0];
		}
		return fOperationTable.values().toArray(new TextEdit[fOperationTable.size()]);
	}

	protected void insertKey(IDocumentKey key, String name) {
		int offset = PropertiesUtil.getInsertOffset(fDocument);
		InsertEdit edit = new InsertEdit(offset, key.write());
		fOperationTable.put(key, edit);
		if (fReadableNames != null) {
			fReadableNames.put(edit, name);
		}
	}

	protected void deleteKey(IDocumentKey key, String name) {
		if (key.getOffset() >= 0) {
			DeleteEdit edit = new DeleteEdit(key.getOffset(), key.getLength());
			fOperationTable.put(key, edit);
			if (fReadableNames != null) {
				fReadableNames.put(edit, name);
			}
		}
	}

	protected void modifyKey(IDocumentKey key, String name) {
		if (key.getOffset() == -1) {
			insertKey(key, name);
		} else {
			ReplaceEdit edit = new ReplaceEdit(key.getOffset(), key.getLength(), key.write());
			fOperationTable.put(key, edit);
			if (fReadableNames != null) {
				fReadableNames.put(edit, name);
			}
		}
	}

	@Override
	public String getReadableName(TextEdit edit) {
		if (fReadableNames != null && fReadableNames.containsKey(edit)) {
			return fReadableNames.get(edit);
		}
		return null;
	}
}
