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

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.AbstractKeyValueTextChangeListener;
import org.eclipse.pde.internal.core.text.IDocumentKey;

public class PropertiesTextChangeListener extends AbstractKeyValueTextChangeListener {

	public PropertiesTextChangeListener(IDocument document) {
		super(document);
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

}
