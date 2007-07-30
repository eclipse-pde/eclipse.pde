/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.NodeDocumentHandler;

/**
 * SimpleCSDocumentHandler
 *
 */
public class SimpleCSDocumentHandler extends NodeDocumentHandler {

	private SimpleCSModel fModel;
	
	/**
	 * @param reconciling
	 */
	public SimpleCSDocumentHandler(SimpleCSModel model, boolean reconciling) {
		super(reconciling, model.getFactory());
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
	protected IDocumentNode getRootNode() {
		return (IDocumentNode)fModel.getSimpleCS();
	}

}
