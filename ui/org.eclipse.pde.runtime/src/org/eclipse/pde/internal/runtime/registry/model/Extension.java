/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

public class Extension extends ModelObject {

	private String namespaceIdentifier;
	private String label;
	private String extensionPointUniqueIdentifier;
	private ConfigurationElement[] configurationElements = new ConfigurationElement[0];
	private Long contributor;

	public void setNamespaceIdentifier(String namespaceIdentifier) {
		this.namespaceIdentifier = namespaceIdentifier;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setExtensionPointUniqueIdentifier(String extensionPointUniqueIdentifier) {
		this.extensionPointUniqueIdentifier = extensionPointUniqueIdentifier;
	}

	public void setConfigurationElements(ConfigurationElement[] configurationElements) {
		if (configurationElements == null)
			throw new IllegalArgumentException();

		this.configurationElements = configurationElements;
	}

	public void setContributor(Long contributor) {
		this.contributor = contributor;
	}

	public ConfigurationElement[] getConfigurationElements() {
		return configurationElements;
	}

	public String getExtensionPointUniqueIdentifier() {
		return extensionPointUniqueIdentifier;
	}

	public String getLabel() {
		return label;
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

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Extension other = (Extension) obj;
		if (contributor == null) {
			if (other.contributor != null)
				return false;
		} else if (!contributor.equals(other.contributor))
			return false;
		if (extensionPointUniqueIdentifier == null) {
			if (other.extensionPointUniqueIdentifier != null)
				return false;
		} else if (!extensionPointUniqueIdentifier.equals(other.extensionPointUniqueIdentifier))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (namespaceIdentifier == null) {
			if (other.namespaceIdentifier != null)
				return false;
		} else if (!namespaceIdentifier.equals(other.namespaceIdentifier))
			return false;
		return true;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contributor == null) ? 0 : contributor.hashCode());
		result = prime * result + ((extensionPointUniqueIdentifier == null) ? 0 : extensionPointUniqueIdentifier.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((namespaceIdentifier == null) ? 0 : namespaceIdentifier.hashCode());
		return result;
	}

	public ExtensionPoint getExtensionPoint() {
		if (model == null)
			return null;
		return model.getExtensionPoint(extensionPointUniqueIdentifier);
	}

	/**
	 * @return contributor or <code>null</code> if contributor not present
	 */
	public Bundle getContributor() {
		if (model == null || contributor == null)
			return null;
		return model.getBundle(contributor);
	}
}
