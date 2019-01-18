/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.build;

import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.text.AbstractKeyValueTextChangeListener;
import org.eclipse.pde.internal.core.text.IDocumentKey;

public class PropertiesTextChangeListener extends AbstractKeyValueTextChangeListener {

	public PropertiesTextChangeListener(IDocument document) {
		super(document, false);
	}

	public PropertiesTextChangeListener(IDocument document, boolean generateReadableNames) {
		super(document, generateReadableNames);
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (Object object : objects) {
			IDocumentKey key = (IDocumentKey) object;
			Object op = fOperationTable.remove(key);
			if (fReadableNames != null) {
				fReadableNames.remove(op);
			}
			String name = null;
			switch (event.getChangeType()) {
				case IModelChangedEvent.REMOVE :
					if (fReadableNames != null) {
						name = NLS.bind(PDECoreMessages.PropertiesTextChangeListener_editNames_remove, key.getName());
					}
					deleteKey(key, name);
					break;
				default :
					if (fReadableNames != null) {
						name = NLS.bind((key.getOffset() == -1 ? PDECoreMessages.PropertiesTextChangeListener_editNames_insert : PDECoreMessages.PropertiesTextChangeListener_editNames_delete), key.getName());
					}
					modifyKey(key, name);
			}
		}
	}

}
