/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public final class RequirementWithChildren implements Requirement {

	private Requirement req;
	private Collection<?> children;

	public RequirementWithChildren(Requirement req, Collection<?> children) {
		this.req = req;
		this.children = children == null ? List.of() : List.copyOf(children);
	}

	@Override
	public String getNamespace() {
		return req.getNamespace();
	}

	@Override
	public Map<String, String> getDirectives() {
		return req.getDirectives();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return req.getAttributes();
	}

	@Override
	public Resource getResource() {
		return req.getResource();
	}

	@Override
	public boolean equals(Object obj) {
		return req.equals(obj);
	}

	@Override
	public int hashCode() {
		return req.hashCode();
	}

	public Collection<?> getChildren() {
		return children;
	}

}