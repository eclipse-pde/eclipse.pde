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
package org.eclipse.pde.internal.core.site;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public abstract class AbstractSiteModel
	extends AbstractModel
	implements ISiteModel {
	protected transient Site site;
	private transient ISiteModelFactory factory;
	private boolean enabled = true;
	private ISiteBuildModel siteBuildModel;

	public AbstractSiteModel() {
		super();
	}
	public ISite getSite() {
		if (site == null) {
			Site s = new Site();
			s.model = this;
			this.site = s;
		}
		return site;
	}
	public ISiteBuildModel getBuildModel() {
		return siteBuildModel;
	}
	public void setBuildModel(ISiteBuildModel buildModel) {
		this.siteBuildModel = buildModel;
	}
	public ISiteModelFactory getFactory() {
		if (factory == null)
			factory = new SiteModelFactory(this);
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
	public boolean isValid() {
		if (!isLoaded() || site==null) return false;
		return site.isValid();
	}
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			InputSource source = new InputSource(stream);
			URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
			source.setSystemId(dtdLocation.toString());
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(source, handler);
			processDocument(handler.getDocument(), handler.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}

	private void processDocument(Document doc, Hashtable lineTable) {
		Node rootNode = doc.getDocumentElement();
		if (site == null) {
			site = new Site();
			site.model = this;
		} else {
			site.reset();
		}
		site.parse(rootNode, lineTable);
	}
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		if (site != null)
			site.reset();
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { site },
				null));
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isReconcilingModel() {
		return false;
	}
}
