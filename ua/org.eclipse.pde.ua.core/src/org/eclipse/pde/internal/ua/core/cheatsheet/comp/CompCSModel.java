/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CompCSModel extends AbstractModel implements ICompCSModel {

	private ICompCSModelFactory fFactory;

	private ICompCS fCompCS;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	public CompCSModel() {
		// NO-OP
	}

	@Override
	protected void updateTimeStamp() {
		// NO-OP
	}

	@Override
	public ICompCS getCompCS() {
		if (fCompCS == null) {
			fCompCS = getFactory().createCompCS();
		}
		return fCompCS;
	}

	@Override
	public ICompCSModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new CompCSModelFactory(this);
		}
		return fFactory;
	}

	@Override
	public boolean isInSync() {
		return true;
	}

	@Override
	public void load() throws CoreException {
		// NO-OP
	}

	@Override
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

	@Override
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {

		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED, new Object[] { fCompCS },
				null));
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	/**
	 * @param doc
	 */
	private void processDocument(Document doc) {
		Element rootNode = doc.getDocumentElement();
		if (fCompCS == null) {
			fCompCS = getFactory().createCompCS();
		} else {
			fCompCS.reset();
		}
		fCompCS.parse(rootNode);
	}

}
