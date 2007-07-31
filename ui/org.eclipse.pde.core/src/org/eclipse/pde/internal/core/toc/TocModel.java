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

package org.eclipse.pde.internal.core.toc;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The TocModel class models a Table of Contents and deals with any
 * related input/output operations.
 *
 */
public class TocModel extends AbstractModel {

	//The data factory used by this model to create TOC objects
	private TocModelFactory fFactory;
	
	//The root TOC element associated with this model
	private Toc fToc;	
	
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new TOC model object. 
	 */
	public TocModel() {
		// NO-OP
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
		// NO-OP
	}

	/**
	 * @return the TOC represented by this model
	 */
	public Toc getToc() {
		//if no TOC exists, create one
		if (fToc == null) {
			fToc = getFactory().createToc();
		}
		return fToc;
	}

	/**
	 * @return the factory used by this model to construct new TOC objects
	 */
	public TocModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new TocModelFactory(this);
		}
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load()
	 */
	public void load() throws CoreException {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync)
			throws CoreException {

		try {
			//Create the parser and handler
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			//Parse the input
			parser.parse(source, handler);
			if (handler.isPrepared()) {
				//Begin the processing of the input
				processDocument(handler.getDocument());
				setLoaded(true);
			}
		} catch (Exception e) {
			setException(e);
			PDECore.logException(e);
		} finally {
			try {
				if (source != null) {
					source.close();
				}
			} catch (IOException e) {
				// Ignore
			}
		}		

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {
		//Reload the model
		load(source, outOfSync);
		//fire a world-changed event to notify listeners that
		//the model has changed
		fireModelChanged(
				new ModelChangedEvent(this,
					IModelChangedEvent.WORLD_CHANGED,
					new Object[] { fToc },
					null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IBaseModel#isEditable()
	 */
	public boolean isEditable() {
		//Since there is no underlying resource, the model cannot be altered
		return false;
	}

	/**
	 * Process the TOC by initializing the root object
	 * and parsing the &lt;toc&gt; root element
	 * 
	 * @param doc The document containing the TOC's XML root element
	 */
	private void processDocument(Document doc) {
		Element rootNode = doc.getDocumentElement();
		if (fToc == null) {
			fToc = getFactory().createToc();
		} else {
			fToc.reset();
		}
		fToc.parse(rootNode);
	}		
	
}
