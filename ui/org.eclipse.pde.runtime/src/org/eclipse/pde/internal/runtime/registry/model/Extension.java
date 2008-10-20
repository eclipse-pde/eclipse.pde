package org.eclipse.pde.internal.runtime.registry.model;

public class Extension extends ModelObject {

	private String namespaceIdentifier;
	private String label;
	private String extensionPointUniqueIdentifier;
	private ConfigurationElement[] configurationElements;
	private Long contributor;

	public Extension(RegistryModel model, String namespaceIdentifier, String label, String extensionPointUniqueIdentifier, ConfigurationElement[] cfg, Long contributor) {
		super(model);
		this.namespaceIdentifier = namespaceIdentifier;
		this.label = label;
		this.extensionPointUniqueIdentifier = extensionPointUniqueIdentifier;
		this.configurationElements = cfg;
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

	public Long getContributorId() {
		return contributor;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Extension) {
			Extension ext = (Extension) obj;
			return namespaceIdentifier.endsWith(ext.namespaceIdentifier) && label.equals(ext.label) && extensionPointUniqueIdentifier.equals(ext.extensionPointUniqueIdentifier) && contributor.equals(ext.contributor);
		}

		return false;
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
		return getModel().getExtensionPoint(extensionPointUniqueIdentifier);
	}

	public Bundle getContributor() {
		return getModel().getBundle(contributor);
	}
}
