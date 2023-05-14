/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractNLModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLCopyrightHandler;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class AbstractFeatureModel extends AbstractNLModel implements IFeatureModel {

	private static final long serialVersionUID = 1L;
	protected transient Feature feature;
	private transient IFeatureModelFactory factory;
	private boolean enabled = true;

	public AbstractFeatureModel() {
		super();
	}

	@Override
	public IFeature getFeature() {
		if (feature == null) {
			Feature f = new Feature();
			f.model = this;
			this.feature = f;
		}
		return feature;
	}

	@Override
	public IFeatureModelFactory getFactory() {
		if (factory == null) {
			factory = new FeatureFactory(this);
		}
		return factory;
	}

	@Override
	public String getInstallLocation() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			XMLCopyrightHandler chandler = new XMLCopyrightHandler(handler);
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", chandler); //$NON-NLS-1$
			parser.parse(stream, handler);
			if (handler.isPrepared()) {
				processDocument(handler.getDocument());
				String copyright = chandler.getCopyright();
				if (copyright != null) {
					feature.setCopyright(copyright);
				}
				setLoaded(true);
				if (!outOfSync) {
					updateTimeStamp();
				}
			}
		} catch (SAXException e) {
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}

	@Override
	public boolean isValid() {
		if (!isLoaded()) {
			return false;
		}
		IFeature feature = getFeature();
		return feature != null && feature.isValid();
	}

	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (feature == null) {
			feature = new Feature();
			feature.model = this;
		} else {
			feature.reset();
		}
		feature.parse(rootNode);
	}

	@Override
	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		if (feature != null) {
			feature.reset();
		}
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {feature}, null));
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
