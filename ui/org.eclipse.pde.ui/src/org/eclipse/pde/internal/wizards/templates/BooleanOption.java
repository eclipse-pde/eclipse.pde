
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

public class BooleanOption extends TemplateOption {
	private Button button;
	
	public BooleanOption(GenericTemplateSection section, String name, String label) {
		super(section, name, label);
	}
	
	public boolean isSelected() {
		return value!=null && value.equals(Boolean.TRUE);
	}
	public void setSelected(boolean selected) {
		setValue(selected?Boolean.TRUE:Boolean.FALSE);
		if (button!=null)
			button.setSelection(selected);
	}
	public void createControl(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		if (factory==null) {
			button = new Button(parent, SWT.CHECK);
			button.setText(getLabel());

		}
		else
			button = factory.createButton(parent, getLabel(), SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		button.setLayoutData(gd);
		button.setSelection(isSelected());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				value = button.getSelection()?Boolean.TRUE:Boolean.FALSE;
				section.validateOptions(BooleanOption.this);
			}
		});
	}
}
