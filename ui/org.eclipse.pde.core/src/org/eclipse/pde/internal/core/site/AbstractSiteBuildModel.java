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

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

public abstract class AbstractSiteBuildModel
	extends AbstractModel
	implements ISiteBuildModel {
	protected transient SiteBuild siteBuild;

	public AbstractSiteBuildModel() {
	}

	public ISiteBuild getSiteBuild() {
		if (siteBuild == null) {
			SiteBuild s = new SiteBuild();
			s.model = this;
			this.siteBuild= s;
		}
		return siteBuild;
	}
	
	public ISiteBuild createSiteBuild() {
		SiteBuild s = new SiteBuild();
		s.model = this;
		s.parent = null;
		return s;
	}
	
	public ISiteBuildFeature createFeature() {
		SiteBuildFeature f = new SiteBuildFeature();
		f.model = this;
		f.parent = getSiteBuild();
		return f;
	}

	public String getInstallLocation() {
		return null;
	}
	public boolean isEditable() {
		return true;
	}
	public boolean isEnabled() {
		return true;
	}
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(stream, handler);
			processDocument(handler.getDocument());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
			throwParseErrorsException(e);
		}
	}

	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (siteBuild == null) {
			siteBuild = new SiteBuild();
			siteBuild.model = this;
		} else {
			siteBuild.reset();
		}
		siteBuild.parse(rootNode);
	}
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		if (siteBuild != null)
			siteBuild.reset();
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { siteBuild },
				null));
	}
	public boolean isReconcilingModel() {
		return false;
	}
}
