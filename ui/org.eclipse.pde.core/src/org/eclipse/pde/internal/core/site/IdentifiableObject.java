/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
import org.w3c.dom.Node;

public class IdentifiableObject extends SiteObject implements IIdentifiable {

	private static final long serialVersionUID = 1L;
	protected String id;

	public String getId() {
		return id;
	}

	protected void parse(Node node) {
		super.parse(node);
		id = getNodeAttribute(node, "id"); //$NON-NLS-1$
	}

	public boolean isValid() {
		return id != null;
	}

	public void setId(String id) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.id;
		this.id = id;
		firePropertyChanged(this, P_ID, oldValue, id);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	protected void reset() {
		super.reset();
		id = null;
	}
}
