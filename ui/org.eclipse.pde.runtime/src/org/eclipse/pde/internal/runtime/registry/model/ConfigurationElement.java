package org.eclipse.pde.internal.runtime.registry.model;

public class ConfigurationElement extends Attribute {

	private Attribute[] elements = new Attribute[0];

	public void setElements(Attribute[] elements) {
		if (elements == null)
			throw new IllegalArgumentException();

		this.elements = elements;
	}

	public Attribute[] getElements() {
		return elements;
	}
}
