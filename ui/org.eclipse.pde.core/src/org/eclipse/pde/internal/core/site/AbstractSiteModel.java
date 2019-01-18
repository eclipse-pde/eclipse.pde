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
package org.eclipse.pde.internal.core.site;

import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.isite.ISiteModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractSiteModel extends AbstractModel implements ISiteModel {
	private static final long serialVersionUID = 1L;
	protected transient Site site;
	private transient ISiteModelFactory factory;
	private boolean enabled = true;

	public AbstractSiteModel() {
		super();
	}

	@Override
	public ISite getSite() {
		if (site == null) {
			Site s = new Site();
			s.model = this;
			this.site = s;
		}
		return site;
	}

	@Override
	public ISiteModelFactory getFactory() {
		if (factory == null) {
			factory = new SiteModelFactory(this);
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
	public boolean isValid() {
		if (!isLoaded() || site == null) {
			return false;
		}
		return site.isValid();
	}

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.parse(stream, handler);
			if (handler.isPrepared()) {
				processDocument(handler.getDocument());
				setLoaded(true);
				if (!outOfSync) {
					updateTimeStamp();
				}
			}
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}

	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (site == null) {
			site = new Site();
			site.model = this;
		} else {
			site.reset();
		}
		site.parse(rootNode);
	}

	@Override
	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		if (site != null) {
			site.reset();
		}
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {site}, null));
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
