package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;

public class LabelField extends TemplateField {
	private Label labelControl;

	/**
	 * Constructor for LabelField.
	 * @param section
	 * @param label
	 */
	public LabelField(GenericTemplateSection section, String label) {
		super(section, label);
	}

	/**
	 * @see TemplateField#createControl(Composite, int, FormWidgetFactory)
	 */
	public void createControl(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		Label label = createLabel(parent, span, factory);

		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
		this.labelControl = label;
	}
	
	public void setLabel(String text) {
		super.setLabel(text);
		if (labelControl!=null)
			labelControl.setText(text);
	}
}