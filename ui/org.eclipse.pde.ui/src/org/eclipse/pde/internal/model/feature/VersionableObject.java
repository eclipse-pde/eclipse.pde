package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.feature.*;

public class VersionableObject extends FeatureObject implements IVersonable {
	protected String id;
	protected String version;

public String getId() {
	return id;
}

public String getVersion() {
	return version;
}

protected void parse(Node node) {
	super.parse(node);
	id = getNodeAttribute(node, "id");
	version = getNodeAttribute(node, "version");
}

public void setId(String id) throws CoreException {
	ensureModelEditable();
	Object oldValue = this.id;
	this.id = id;
	firePropertyChanged(this, P_ID, oldValue, id);
}
public void setVersion(String version) throws CoreException {
	ensureModelEditable();
	Object oldValue = this.version;
	this.version = version;
	firePropertyChanged(this, P_VERSION, oldValue, version);
}

protected void reset() {
	super.reset();
	id = null;
	version = null;
}
}
