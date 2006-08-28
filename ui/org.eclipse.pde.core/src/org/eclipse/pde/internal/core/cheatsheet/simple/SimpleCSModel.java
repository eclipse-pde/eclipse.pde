/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * SimpleCSModel
 *
 */
public class SimpleCSModel extends AbstractModel implements ISimpleCSModel {

	private ISimpleCSModelFactory fFactory;
	
	private ISimpleCS fSimpleCS;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSModel() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#updateTimeStamp()
	 */
	protected void updateTimeStamp() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel#getFactory()
	 */
	public ISimpleCSModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new SimpleCSModelFactory(this);
		}
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel#getSimpleCS()
	 */
	public ISimpleCS getSimpleCS() {
		if (fSimpleCS == null) {
			fSimpleCS = getFactory().createSimpleCS();
		}
		return fSimpleCS;
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
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.parse(source, handler);
			if (handler.isPrepared()) {
				processDocument(handler.getDocument());
				setLoaded(true);
			}
		} catch (Exception e) {
			PDECore.logException(e);
		} finally {
			try {
				if (source != null)
					source.close();
			} catch (IOException e) {
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {
		load(source, outOfSync);
		fireModelChanged(
				new ModelChangedEvent(this,
					IModelChangedEvent.WORLD_CHANGED,
					new Object[] { fSimpleCS },
					null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IBaseModel#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}

	/**
	 * @param doc
	 */
	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (fSimpleCS == null) {
			fSimpleCS = getFactory().createSimpleCS();
		} else {
			fSimpleCS.reset();
		}
		fSimpleCS.parse(rootNode);
	}	
	
}
