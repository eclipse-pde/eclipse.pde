package org.eclipse.pde.internal.runtime.registry.model;

import java.util.ArrayList;
import java.util.List;

public class ExtensionPoint extends ModelObject {

	private String label;
	private String uniqueIdentifier;
	private String namespaceIdentifier;
	private Long contributor;
	private List extensions = new ArrayList();

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

	public void setExtensions(List extensions) {
		this.extensions = extensions;
	}

	public List getExtensions() {
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
		if (model == null || contributor == null)
			return null;
		return model.getBundle(contributor);
	}

	public boolean equals(Object obj) {
		return (obj instanceof ExtensionPoint) && (uniqueIdentifier.equals(((ExtensionPoint) obj).uniqueIdentifier));
	}

	public int hashCode() {
		return uniqueIdentifier.hashCode();
	}
}
