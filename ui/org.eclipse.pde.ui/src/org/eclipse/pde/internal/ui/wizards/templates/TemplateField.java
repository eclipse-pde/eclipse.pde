
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.swt.SWT;

public abstract class TemplateField {
	private OptionTemplateSection section;
	private String label;
	
	public TemplateField(OptionTemplateSection section, String label) {
		this.section = section;
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public OptionTemplateSection getSection() {
		return section;
	}
	
	protected Label createLabel(Composite parent, int span, FormWidgetFactory factory) {
		Label label;
		if (factory != null) {
			label = factory.createLabel(parent, getLabel());
		} else {
			label = new Label(parent, SWT.NULL);
			label.setText(getLabel());
		}
		return label;
	}
	
	public abstract void createControl(Composite parent, int span, FormWidgetFactory factory);
}