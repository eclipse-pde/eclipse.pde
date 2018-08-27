/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	@Override
	protected IDocumentElementNode getRootNode() {
		return (IDocumentElementNode) fModel.getRoot();
	}
}
