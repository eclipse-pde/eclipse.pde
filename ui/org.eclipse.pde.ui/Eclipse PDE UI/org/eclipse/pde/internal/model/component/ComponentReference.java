package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;

public class ComponentReference extends ComponentObject implements IComponentReference {
	private String id;
	private String version;

public String getId() {
	return id;
}
public String getVersion() {
	return version;
}
void parse(Node node) {
	id = getNodeAttribute(node, "id");
	label = getNodeAttribute(node, "label");
	version = getNodeAttribute(node, "version");
}
public void setId(String id) throws CoreException {
	ensureModelEditable();
	this.id = id;
	firePropertyChanged(this, P_ID);
}
public void setVersion(String version) throws CoreException {
	ensureModelEditable();
	this.version = version;
	firePropertyChanged(this, P_VERSION);
}
}
