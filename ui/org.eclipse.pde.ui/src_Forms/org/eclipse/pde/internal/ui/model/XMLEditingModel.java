/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model;

import java.io.*;

import javax.xml.parsers.*;

import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public abstract class XMLEditingModel extends AbstractEditingModel {
	
	private SAXParser fParser;

	public XMLEditingModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) {
		try {
			fLoaded = true;
			getParser().parse(source, createDocumentHandler(this));
		} catch (SAXException e) {
			fLoaded = false;
		} catch (IOException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#adjustOffsets(org.eclipse.jface.text.IDocument)
	 */
	protected void adjustOffsets(IDocument document) {
		try {
			getParser().parse(getInputStream(document), createNodeOffsetHandler(this));
		} catch (SAXException e) {
		} catch (IOException e) {
		}
	}
	
	protected abstract DefaultHandler createNodeOffsetHandler(IModel model);
		
	protected abstract DefaultHandler createDocumentHandler(IModel model);
	
	private SAXParser getParser() {
		try {
			if (fParser == null) {
				fParser = SAXParserFactory.newInstance().newSAXParser();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fParser;
	}
	
}
