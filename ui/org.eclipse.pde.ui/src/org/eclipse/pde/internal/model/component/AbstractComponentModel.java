package org.eclipse.pde.internal.model.component;
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
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.PDEPlugin;

public abstract class AbstractComponentModel extends AbstractModel implements IComponentModel {
	protected Component component;
	private IComponentModelFactory factory;
	private boolean enabled=true;

public AbstractComponentModel() {
	super();
}
public IComponent getComponent() {
	if (component==null) {
		Component c = new Component();
		c.model = this;
		this.component = c;
	}
	return component;
}
public IComponentModelFactory getFactory() {
	if (factory==null) factory = new ComponentFactory(this);
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
	if (component == null) {
		component = new Component();
		component.model = this;
	} else {
		component.reset();
	}
	component.parse(rootNode);
}
public void reload(InputStream stream, boolean outOfSync) throws CoreException {
	if (component != null)
		component.reset();
	load(stream, outOfSync);
	fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.WORLD_CHANGED,
			new Object[] { component },
			null));
}
public void setEnabled(boolean enabled) {
	this.enabled = enabled;
}
}
