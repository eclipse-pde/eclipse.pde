package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;

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
		SourceDOMParser parser = new SourceDOMParser();
		XMLErrorHandler errorHandler = new XMLErrorHandler();
		parser.setErrorHandler(errorHandler);

		try {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
		}
		catch (SAXException e) {
		}

		try {
			InputSource source = new InputSource(stream);
			URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
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
		if (feature == null) {
			feature = new Feature();
			feature.model = this;
		} else {
			feature.reset();
		}
		feature.parse(rootNode);
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
}