/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TargetModel extends AbstractModel implements ITargetModel {

	private static final long serialVersionUID = 1L;
	
	private ITargetModelFactory fFactory;
	private ITarget fTarget;

	protected void updateTimeStamp() {
	}

	public ITarget getTarget() {
		if (fTarget == null)
			fTarget = getFactory().createTarget();
		return fTarget;
	}

	public ITargetModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new TargetModelFactory(this);
		return fFactory;
	}

	public String getInstallLocation() {
		return null;
	}

	public boolean isInSync() {
		return true;
	}

	public void load() throws CoreException {
	}

	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.parse(stream, handler);
			if (handler.isPrepared()) {
				processDocument(handler.getDocument());
				setLoaded(true);
			}
		} catch (Exception e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}
		}
	}

	public void reload(InputStream source, boolean outOfSync) throws CoreException {
		load(source, outOfSync);
		fireModelChanged(
				new ModelChangedEvent(this,
					IModelChangedEvent.WORLD_CHANGED,
					new Object[] { fTarget },
					null));
	}

	public boolean isEditable() {
		return false;
	}
	
	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (fTarget == null) {
			fTarget = getFactory().createTarget();
		} else {
			fTarget.reset();
		}
		fTarget.parse(rootNode);
	}


}
