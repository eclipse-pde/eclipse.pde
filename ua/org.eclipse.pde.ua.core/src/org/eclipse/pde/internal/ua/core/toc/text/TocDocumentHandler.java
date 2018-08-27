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

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.NodeDocumentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class TocDocumentHandler extends NodeDocumentHandler {

	private TocModel fModel;

	/**
	 * @param reconciling
	 */
	public TocDocumentHandler(TocModel model, boolean reconciling) {
		super(reconciling, model.getFactory());
		fModel = model;
	}

	@Override
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	@Override
	protected IDocumentElementNode getRootNode() {
		return fModel.getToc();
	}

	@Override
	public void startDocument() throws SAXException {
		//starting fresh parsing, clean the known errors
		fModel.purgeErrors();
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		//reached the document end, refresh the markers (if any)
		super.endDocument();
		if (fModel.isMarkerRefreshNeeded()) {
			TocMarkerManager.refreshMarkers(fModel);
			fModel.setMarkerRefreshNeeded(false);
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		//error are recoverable so add it and continue
		fModel.addError(e);
		super.error(e);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		//fatalError are not recoverable, so add it and refresh the marker as the document won't be parsed further
		fModel.addError(e);
		super.fatalError(e);
		if (fModel.isMarkerRefreshNeeded()) {
			TocMarkerManager.refreshMarkers(fModel);
			fModel.setMarkerRefreshNeeded(false);
		}
	}

}
