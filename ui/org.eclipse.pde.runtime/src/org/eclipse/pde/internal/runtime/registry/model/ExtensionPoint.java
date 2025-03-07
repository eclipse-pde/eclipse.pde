/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.ArrayList;
import java.util.List;

public class ExtensionPoint extends ModelObject {

	private String label;
	private String uniqueIdentifier;
	private String namespaceIdentifier;
	private Long contributor;
	private List<Extension> extensions = new ArrayList<>();

	public void setLabel(String label) {
		this.label = label;
	}

	public void setUniqueIdentifier(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	public void setNamespaceIdentifier(String namespaceIdentifier) {
		this.namespaceIdentifier = namespaceIdentifier;
	}

	public void setContributor(Long contributor) {
		this.contributor = contributor;
	}

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}

	public List<Extension> getExtensions() {
		return extensions;
	}

	public String getLabel() {
		return label;
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public String getNamespaceIdentifier() {
		return namespaceIdentifier;
	}

	/**
	 * @return contributor id or <code>null</code> if contributor not present
	 */
	public Long getContributorId() {
		return contributor;
	}

	/**
	 * @return contributor or <code>null</code> if contributor not present
	 */
	public Bundle getContributor() {
		if (model == null || contributor == null) {
			return null;
		}
		return model.getBundle(contributor);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueIdentifier == null) ? 0 : uniqueIdentifier.hashCode());
		return result;
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
		ExtensionPoint other = (ExtensionPoint) obj;
		if (uniqueIdentifier == null) {
			if (other.uniqueIdentifier != null) {
				return false;
			}
		} else if (!uniqueIdentifier.equals(other.uniqueIdentifier)) {
			return false;
		}
		return true;
	}
}
