package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
import org.w3c.dom.Node;

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
	
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue!=null ? newValue.toString() : null);
		}
		else super.restoreProperty(name, oldValue, newValue);
	}

	protected void reset() {
		super.reset();
		id = null;
	}
}