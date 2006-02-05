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

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.AbstractTextChangeListener;

public class BundleTextChangeListener extends AbstractTextChangeListener {

	public BundleTextChangeListener(IDocument document) {
		super(document);
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
            if (object instanceof PDEManifestElement)
                object = ((PDEManifestElement)object).getHeader();
            else if (object instanceof PackageFriend)
                object = ((PackageFriend)object).getHeader();
            
			if (object instanceof ManifestHeader) {
				ManifestHeader header = (ManifestHeader)object;
				fOperationTable.remove(header);
				
				if (header.getValue() == null || header.getValue().trim().length() == 0) {
					deleteKey(header);						
				} else {
					modifyKey(header);
				}
			}
		}
	}

}
