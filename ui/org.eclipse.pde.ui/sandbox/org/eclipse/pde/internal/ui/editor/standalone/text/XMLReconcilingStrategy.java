/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.standalone.text;


import java.io.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;


public class XMLReconcilingStrategy implements IReconcilingStrategy {
	private DocumentModel fModel;
	private IDocument fDocument;

	public XMLReconcilingStrategy(DocumentModel model) {
		fModel = model;
	}

	public void reconcile(IRegion partition) {
		internalReconcile();
	}

	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		internalReconcile();
	}
	
	private synchronized void internalReconcile() {
		if (fDocument != null && fModel != null) {
			try {
				fModel.reconcile(
					new ByteArrayInputStream(fDocument.get().getBytes("UTF8")));
			} catch (UnsupportedEncodingException e) {
			}
		}
	}
	
	public synchronized void setDocument(IDocument document) {
		fDocument = document;
	}

}
