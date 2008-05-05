/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.core.text.NodeDocumentHandler;

/**
 * Document handler for declarative services xml files.
 * 
 * @since 3.4
 * @see DSModel
 * @see DSDocumentFactory
 */
public class DSDocumentHandler extends NodeDocumentHandler {

	private DSModel fModel;

	public DSDocumentHandler(DSModel model, boolean reconciling) {
		super(reconciling, (IDocumentNodeFactory) model.getFactory());
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.NodeDocumentHandler#getRootNode()
	 */
	protected IDocumentElementNode getRootNode() {
		return (IDocumentElementNode) fModel.getRoot();
	}
}
