package org.eclipse.pde.internal.runtime.registry.model;

public class ConfigurationElement extends Attribute {

	private Attribute[] elements;

	public ConfigurationElement(RegistryModel model, String name, Attribute[] attributes) {
		super(model, name, null);
		elements = attributes;
	}

	public Attribute[] getElements() {
		return elements;
	}
}
