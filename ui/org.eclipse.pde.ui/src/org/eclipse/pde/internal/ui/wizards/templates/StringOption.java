
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

public class StringOption extends TemplateOption {
	private Text text;
	private Label labelControl;
	private boolean ignoreListener;
	
	public StringOption(GenericTemplateSection section, String name, String label) {
		super(section, name, label);
		setRequired(true);
	}
	
	public String getText() {
		if (getValue()!=null) return getValue().toString();
		return null;
	}
	public void setText(String newText) {
		setValue(newText);
		if (text!=null)
			text.setText(newText);
	}
	public void setValue(Object value) {
		super.setValue(value);
		if (text!=null) {
			ignoreListener = true;
			String textValue = getText();
			text.setText(textValue!=null?textValue:"");
			ignoreListener = false;
		}
	}
	public void createControl(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		
		labelControl = createLabel(parent, 1, factory);
		labelControl.setEnabled(isEnabled());
		if (factory==null) {
			text = new Text(parent, SWT.SINGLE | SWT.BORDER);
			if (getValue()!=null) text.setText(getValue().toString());
		}
		else {
			text = factory.createText(parent, getText(), SWT.SINGLE);
		}
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span-1;
		text.setLayoutData(gd);
		text.setEnabled(isEnabled());
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (ignoreListener) return;
				StringOption.super.setValue(text.getText());
				getSection().validateOptions(StringOption.this);
			}
		});
	}
	
	public boolean isEmpty() {
		return getValue()==null || getValue().toString().length()==0;
	}
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (labelControl!=null) {
			labelControl.setEnabled(enabled);
			text.setEnabled(enabled);
		}
	}
}
