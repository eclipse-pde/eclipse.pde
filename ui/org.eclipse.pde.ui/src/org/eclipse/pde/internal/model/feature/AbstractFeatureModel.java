package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.apache.xerces.parsers.*;
import org.eclipse.pde.internal.base.model.*;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.PDEPlugin;

public abstract class AbstractFeatureModel extends AbstractModel implements IFeatureModel {
	protected Feature feature;
	private IFeatureModelFactory factory;
	private boolean enabled=true;

public AbstractFeatureModel() {
	super();
}
public IFeature getFeature() {
	if (feature==null) {
		Feature f = new Feature();
		f.model = this;
		this.feature = f;
	}
	return feature;
}
public IFeatureModelFactory getFactory() {
	if (factory==null) factory = new FeatureFactory(this);
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
	DOMParser parser = new DOMParser();
	try {
		InputSource source = new InputSource(stream);
		parser.parse(source);
		processDocument(parser.getDocument());
		loaded=true;
		if (!outOfSync) updateTimeStamp();
	} catch (SAXException e) {
	} catch (IOException e) {
		PDEPlugin.logException(e);
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
public void reload(InputStream stream, boolean outOfSync) throws CoreException {
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
