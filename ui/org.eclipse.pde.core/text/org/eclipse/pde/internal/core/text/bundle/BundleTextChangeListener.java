/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.text.AbstractKeyValueTextChangeListener;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.util.PropertiesUtil;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

public class BundleTextChangeListener extends AbstractKeyValueTextChangeListener {

	public BundleTextChangeListener(IDocument document) {
		super(document, false);
	}

	public BundleTextChangeListener(IDocument document, boolean generateReadableNames) {
		super(document, generateReadableNames);
	}

	public TextEdit[] getTextOperations() {
		TextEdit[] ops = super.getTextOperations();
		try {
			if (ops.length == 0 || !PropertiesUtil.isNewlineNeeded(fDocument))
				return ops;
		} catch (BadLocationException e) {
		}

		TextEdit[] result = new TextEdit[ops.length + 1];
		result[ops.length] = new InsertEdit(PropertiesUtil.getInsertOffset(fDocument), fSep);
		if (fReadableNames != null)
			fReadableNames.put(result[ops.length], PDECoreMessages.BundleTextChangeListener_editNames_newLine);
		System.arraycopy(ops, 0, result, 0, ops.length);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * This method is overwritten in the BundleTextChangeListener so that newly inserted headers
	 * will have their separator inserted at the start of the change rather than the end when there
	 * is not already a new line at the end of the manifest. This allows the "Add a new line at the
	 * end of the file" change to go at the bottom of the preview as the user would expect. Previously
	 * it was added before all inserts so the new headers would appear on new lines.
	 * 
	 * @see org.eclipse.pde.internal.core.text.AbstractKeyValueTextChangeListener#insertKey(org.eclipse.pde.internal.core.text.IDocumentKey, java.lang.String)
	 */
	protected void insertKey(IDocumentKey key, String name) {
		int offset = PropertiesUtil.getInsertOffset(fDocument);
		StringBuffer buffer = new StringBuffer(key.write());
		try {
			// if the file does not end in a new line and the key to insert does, move the new line
			// to the start of the key.
			if (PropertiesUtil.isNewlineNeeded(fDocument) && buffer.substring(buffer.length() - fSep.length()).equals(fSep)) {
				buffer.insert(0, fSep);
				buffer.setLength(buffer.length() - fSep.length());
			}
		} catch (BadLocationException e) {
		}
		InsertEdit edit = new InsertEdit(offset, buffer.toString());
		fOperationTable.put(key, edit);
		if (fReadableNames != null)
			fReadableNames.put(edit, name);
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof PDEManifestElement)
				object = ((PDEManifestElement) object).getHeader();
			else if (object instanceof PackageFriend)
				object = ((PackageFriend) object).getHeader();

			if (object instanceof ManifestHeader) {
				ManifestHeader header = (ManifestHeader) object;
				Object op = fOperationTable.remove(header);
				if (fReadableNames != null)
					fReadableNames.remove(op);

				if (header.getValue() == null || header.getValue().trim().length() == 0) {
					String name = fReadableNames == null ? null : NLS.bind(PDECoreMessages.BundleTextChangeListener_editNames_remove, header.fName);
					deleteKey(header, name);
				} else {
					String name = fReadableNames == null ? null : NLS.bind(header.getOffset() == -1 ? PDECoreMessages.BundleTextChangeListener_editNames_insert : PDECoreMessages.BundleTextChangeListener_editNames_modify, header.fName);
					modifyKey(header, name);
				}
			}
		}
	}

}
