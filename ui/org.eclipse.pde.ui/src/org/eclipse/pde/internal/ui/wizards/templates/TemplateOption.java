
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class TemplateOption extends TemplateField {
	private String name;
	private Object value;
	private boolean enabled=true;
	private boolean required;
	
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
	
	public void setRequired(boolean required) {
		this.required = required;
	}
	public boolean isRequired() {
		return required;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getMessageLabel() {
		String label = getLabel();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<label.length(); i++) {
			char c = label.charAt(i);
			if (c!='&' && c!=':') buf.append(c);
		}
		return buf.toString();
	}
}