/*******************************************************************************
 * Copyright (c) 2014, 2023 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.views.resolution;

import java.util.Objects;

import org.eclipse.core.runtime.IAdaptable;
import org.osgi.resource.Requirement;

public class RequirementWrapper implements IAdaptable {

	public final Requirement			requirement;
	public boolean						resolved;
	public boolean						java;

	public RequirementWrapper(Requirement requirement) {
		this.requirement = requirement;
	}

	@Override
	public int hashCode() {
		return Objects.hash(java, requirement, resolved);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RequirementWrapper other = (RequirementWrapper) obj;
		return java == other.java && Objects.equals(requirement, other.requirement)
				&& resolved == other.resolved;
	}

	@Override
	public String toString() {
		return "RequirementWrapper [resolved=" + resolved + ", java=" + java + ", requirement=" + requirement + "]";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == Requirement.class) {
			return adapter.cast(requirement);
		}
		return null;
	}

}
