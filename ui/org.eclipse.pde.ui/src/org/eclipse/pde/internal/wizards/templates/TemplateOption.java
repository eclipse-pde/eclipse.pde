
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class TemplateOption {
	GenericTemplateSection section;
	String name;
	String label;
	Object value;
	
	public TemplateOption(GenericTemplateSection section, String name, String label) {
		this.section = section;
		this.name = name;
		this.label = label;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
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
	public abstract void createControl(Composite parent, int span, FormWidgetFactory factory);
}