/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
import org.w3c.dom.Node;

public class IdentifiableObject extends FeatureObject implements IIdentifiable {
	private static final long serialVersionUID = 1L;
	protected String id;

	@Override
	public String getId() {
		return id;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		id = getNodeAttribute(node, "id"); //$NON-NLS-1$
	}

	@Override
	public void setId(String id) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.id;
		this.id = id;
		firePropertyChanged(this, P_ID, oldValue, id);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
	protected void reset() {
		super.reset();
		id = null;
	}
}
