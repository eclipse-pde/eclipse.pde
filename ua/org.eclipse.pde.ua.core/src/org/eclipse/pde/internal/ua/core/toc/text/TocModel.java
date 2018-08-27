/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.core.toc.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

public class TocModel extends XMLEditingModel {

	private TocDocumentHandler fHandler;

	private TocDocumentFactory fFactory;

	private Toc fToc;

	private List<Exception> fErrors;

	private boolean fMarkerRefreshNeeded;

	/**
	 * @param document
	 * @param isReconciling
	 */
	public TocModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);

		fHandler = null;
		fFactory = new TocDocumentFactory(this);
		fToc = null;
	}

	@Override
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {

		if (fHandler == null) {
			fHandler = new TocDocumentHandler(this, reconciling);
		}
		return fHandler;
	}

	@Override
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
	public TocDocumentFactory getFactory() {
		return fFactory;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.simple.ITocModel#getToc()
	 */
	public Toc getToc() {
		if (fToc == null) {
			fToc = getFactory().createToc();
		}
		return fToc;
	}

	@Override
	protected IWritable getRoot() {
		return getToc();
	}

	public TocDocumentHandler getDocumentHandler() {
		return fHandler;
	}

	public void addError(Exception e) {
		if (fErrors == null) {
			fErrors = new ArrayList<>(1);
		}

		if (!fErrors.contains(e)) {
			fErrors.add(e);
		}
	}

	public Collection<Exception> getErrors() {
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
