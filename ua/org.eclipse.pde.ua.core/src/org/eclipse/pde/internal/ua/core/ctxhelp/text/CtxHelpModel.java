/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.ctxhelp.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Model describing the elements of a context help xml file.
 * 
 * @since 3.4
 * @see CtxHelpRoot
 * @see CtxHelpDocumentFactory
 * @see CtxHelpDocumentHandler
 */
public class CtxHelpModel extends XMLEditingModel {

	private CtxHelpDocumentHandler fHandler;
	private CtxHelpDocumentFactory fFactory;
	private CtxHelpRoot fRoot;
	private List fErrors;
	private boolean fMarkerRefreshNeeded;

	public CtxHelpModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.XMLEditingModel#createDocumentHandler
	 * (org.eclipse.pde.core.IModel, boolean)
	 */
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null) {
			fHandler = new CtxHelpDocumentHandler(this, reconciling);
		}
		return fHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.AbstractEditingModel#
	 * createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		// Not needed
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ITocModel#getFactory()
	 */
	public CtxHelpDocumentFactory getFactory() {
		if (fFactory == null) {
			fFactory = new CtxHelpDocumentFactory(this);
		}
		return fFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ITocModel#getToc()
	 */
	public CtxHelpRoot getCtxHelpRoot() {
		if (fRoot == null) {
			fRoot = getFactory().createRoot();
		}
		return fRoot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.XMLEditingModel#getRoot()
	 */
	protected IWritable getRoot() {
		return getCtxHelpRoot();
	}

	public void addError(Exception e) {
		if (fErrors == null) {
			fErrors = new ArrayList(1);
		}
		if (!fErrors.contains(e)) {
			fErrors.add(e);
		}
	}

	public Collection getErrors() {
		return fErrors;
	}

	public void purgeErrors() {
		if (fErrors != null) {
			fErrors.clear();
		}
	}

	public void setMarkerRefreshNeeded(boolean refresh) {
		this.fMarkerRefreshNeeded = refresh;
	}

	public boolean isMarkerRefreshNeeded() {
		return fMarkerRefreshNeeded;
	}
}
