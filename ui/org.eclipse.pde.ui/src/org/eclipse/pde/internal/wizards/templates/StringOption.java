
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

public class StringOption extends TemplateOption {
	private Text text;
	
	public StringOption(GenericTemplateSection section, String name, String label) {
		super(section, name, label);
	}
	
	public String getText() {
		if (value!=null) return value.toString();
		return null;
	}
	public void setText(String newText) {
		setValue(newText);
		if (text!=null)
			text.setText(newText);
	}
	public void createControl(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		if (factory==null) {
			Label label = new Label(parent, SWT.NULL);
			label.setText(getLabel());
			text = new Text(parent, SWT.SINGLE | SWT.BORDER);
			if (value!=null) text.setText(value.toString());
		}
		else {
			factory.createLabel(parent, getLabel());
			text = factory.createText(parent, getText(), SWT.SINGLE);
		}
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span-1;
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				value = text.getText();
				section.validateOptions(StringOption.this);
			}
		});
	}
	
	public boolean isEmpty() {
		return value==null || value.toString().length()==0;
	}
}
