
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class TemplateOption extends TemplateField {
	private String name;
	private Object value;
	
	public TemplateOption(GenericTemplateSection section, String name, String label) {
		super(section, label);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public Object getValue() {
		return value;
	}
	
	public boolean isEmpty() {
		return false;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
}