package org.eclipse.pde.internal.runtime.registry.model;

import java.util.ArrayList;
import java.util.List;

public class ExtensionPoint extends ModelObject {

	private String label;
	private String uniqueIdentifier;
	private String namespaceIdentifier;
	private Long contributor;
	private List extensions = new ArrayList();

	public ExtensionPoint(RegistryModel model, String label, String uniqueIdentifier, String namespaceIdentifier, Long contributor) {
		super(model);
		this.label = label;
		this.uniqueIdentifier = uniqueIdentifier;
		this.namespaceIdentifier = namespaceIdentifier;
		this.contributor = contributor;
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

	public Long getContributorId() {
		return contributor;
	}

	public Bundle getContributor() {
		return getModel().getBundle(contributor);
	}

	public boolean equals(Object obj) {
		return (obj instanceof ExtensionPoint) && (uniqueIdentifier.equals(((ExtensionPoint) obj).uniqueIdentifier));
	}

	public int hashCode() {
		return uniqueIdentifier.hashCode();
	}
}
