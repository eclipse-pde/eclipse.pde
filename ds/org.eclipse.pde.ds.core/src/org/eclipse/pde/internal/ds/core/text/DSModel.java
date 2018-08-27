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
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Model describing the elements of a declarative services xml file.
 *
 * @since 3.4
 */
public class DSModel extends XMLEditingModel implements IDSModel {

	private DSDocumentHandler fHandler;
	private IDSDocumentFactory fFactory;
	private IDSComponent fComponent;

	public DSModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	@Override
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null) {
			fHandler = new DSDocumentHandler(this, reconciling);
		}
		return fHandler;
	}

	@Override
	public IDSDocumentFactory getFactory() {
		if (fFactory == null) {
			fFactory = new DSDocumentFactory(this);
		}
		return fFactory;
	}

	@Override
	public IDSComponent getDSComponent() {
		if (fComponent == null) {
			fComponent = getFactory().createComponent();
		}
		return fComponent;
	}

	@Override
	protected IWritable getRoot() {
		return (IWritable) getDSComponent();
	}

}
