/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public abstract class AbstractFeatureModel
	extends AbstractModel
	implements IFeatureModel {
	protected transient Feature feature;
	private transient IFeatureModelFactory factory;
	private boolean enabled = true;

	public AbstractFeatureModel() {
		super();
	}
	public IFeature getFeature() {
		if (feature == null) {
			Feature f = new Feature();
			f.model = this;
			this.feature = f;
		}
		return feature;
	}
	public IFeatureModelFactory getFactory() {
		if (factory == null)
			factory = new FeatureFactory(this);
		return factory;
	}
	public String getInstallLocation() {
		return null;
	}
	public boolean isEditable() {
		return true;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			InputSource source = new InputSource(stream);
			URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
			source.setSystemId(dtdLocation.toString());
			XMLDefaultHandler handler = new XMLDefaultHandler(stream);
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(new InputSource(new StringReader(handler.getText())), handler);
			processDocument(handler.getDocument(), handler.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}
	
	public boolean isValid() {
		if (!isLoaded()) return false;
		IFeature feature = getFeature();
		return feature!=null && feature.isValid();
	}

	private void processDocument(Document doc, Hashtable lineTable) {
		Node rootNode = doc.getDocumentElement();
		if (feature == null) {
			feature = new Feature();
			feature.model = this;
		} else {
			feature.reset();
		}
		feature.parse(rootNode, lineTable);
	}
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		if (feature != null)
			feature.reset();
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { feature },
				null));
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isReconcilingModel() {
		return false;
	}
}
