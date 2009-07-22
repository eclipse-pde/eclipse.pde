/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.NodeDocumentHandler#getRootNode()
	 */
	protected IDocumentElementNode getRootNode() {
		return (IDocumentElementNode) fModel.getToc();
	}

	public void startDocument() throws SAXException {
		//starting fresh parsing, clean the known errors
		fModel.purgeErrors();
		super.startDocument();
	}

	public void endDocument() throws SAXException {
		//reached the document end, refresh the markers (if any)
		super.endDocument();
		if (fModel.isMarkerRefreshNeeded()) {
			TocMarkerManager.refreshMarkers(fModel);
			fModel.setMarkerRefreshNeeded(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.DocumentHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException e) throws SAXException {
		//error are recoverable so add it and continue
		fModel.addError(e);
		super.error(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.DocumentHandler#fatalError(org.xml.sax.SAXParseException)
	 */
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
