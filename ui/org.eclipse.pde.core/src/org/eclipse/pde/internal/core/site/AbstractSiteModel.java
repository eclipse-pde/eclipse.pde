package org.eclipse.pde.internal.core.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
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
	public void load(InputStream stream, boolean outOfSync)
		throws CoreException {
		SourceDOMParser parser = new SourceDOMParser();
		XMLErrorHandler errorHandler = new XMLErrorHandler();
		parser.setErrorHandler(errorHandler);

		try {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature(
				"http://apache.org/xml/features/validation/dynamic",
				true);
		} catch (SAXException e) {
		}
		try {
			InputSource source = new InputSource(stream);
			URL dtdLocation =
				PDECore.getDefault().getDescriptor().getInstallURL();
			source.setSystemId(dtdLocation.toString());
			parser.parse(source);
			if (errorHandler.getErrorCount() > 0
				|| errorHandler.getFatalErrorCount() > 0) {
				throwParseErrorsException();
			}
			processDocument(parser.getDocument());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (SAXException e) {
		} catch (IOException e) {
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
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		if (site != null)
			site.reset();
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { site },
				null));
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}