/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.NodeDocumentHandler;

public class SimpleCSDocumentHandler extends NodeDocumentHandler {

	private SimpleCSModel fModel;

	/**
	 * @param reconciling
	 */
	public SimpleCSDocumentHandler(SimpleCSModel model, boolean reconciling) {
		super(reconciling, model.getFactory());
		fModel = model;
		fCollapsibleParentName = null;
	}

	@Override
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	@Override
	protected IDocumentElementNode getRootNode() {
		return fModel.getSimpleCS();
	}

}
