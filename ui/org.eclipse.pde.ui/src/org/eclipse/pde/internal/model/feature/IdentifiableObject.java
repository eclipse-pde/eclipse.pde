package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.model.IIdentifiable;

public class IdentifiableObject extends FeatureObject implements IIdentifiable {
	protected String id;

	public String getId() {
		return id;
	}

	protected void parse(Node node) {
		super.parse(node);
		id = getNodeAttribute(node, "id");
	}

	public void setId(String id) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.id;
		this.id = id;
		firePropertyChanged(this, P_ID, oldValue, id);
	}

	protected void reset() {
		super.reset();
		id = null;
	}
}