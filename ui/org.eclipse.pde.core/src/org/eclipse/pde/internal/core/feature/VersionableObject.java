package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IVersonable;
import org.w3c.dom.Node;

public class VersionableObject
	extends IdentifiableObject
	implements IVersonable {
	protected String version;

	public String getVersion() {
		return version;
	}

	protected void parse(Node node) {
		super.parse(node);
		version = getNodeAttribute(node, "version");
	}

	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.version;
		this.version = version;
		firePropertyChanged(this, P_VERSION, oldValue, version);
	}

	protected void reset() {
		super.reset();
		version = null;
	}
}