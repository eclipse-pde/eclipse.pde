package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class ChoiceOption extends TemplateOption {
	private String[][] choices;
	private Control labelControl;
	private Button[] buttons;
	private boolean blockListener;

	/**
	 * Constructor for ChoiceOption.
	 * @param section
	 * @param name
	 * @param label
	 */
	public ChoiceOption(
		GenericTemplateSection section,
		String name,
		String label,
		String[][] choices) {
		super(section, name, label);
		this.choices = choices;
	}

	/**
	 * @see TemplateField#createControl(Composite, int, FormWidgetFactory)
	 */
	public void createControl(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		Composite container = createComposite(parent, span, factory);
		fill(container, span);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		labelControl = createLabel(container, span, factory);
		labelControl.setEnabled(isEnabled());
		fill(labelControl, span);

		buttons = new Button[choices.length];

		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.widget;
				if (blockListener)
					return;
				if (b.getSelection()) {
					ChoiceOption.super.setValue(b.getData().toString());
					getSection().validateOptions(ChoiceOption.this);
				}
			}
		};

		for (int i = 0; i < choices.length; i++) {
			String[] choice = choices[i];
			Button button = createRadioButton(parent, span, factory, choice);
			buttons[i] = button;
			button.addSelectionListener(listener);
			button.setEnabled(isEnabled());
		}
		if (getChoice() != null)
			selectChoice(getChoice());
	}

	public String getChoice() {
		return getValue() != null ? getValue().toString() : null;
	}

	private GridData fill(Control control, int span) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
		return gd;
	}

	private Composite createComposite(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		Composite composite;
		if (factory == null)
			composite = new Composite(parent, SWT.NULL);
		else
			composite = factory.createComposite(parent);
		fill(composite, span);
		return composite;
	}

	private Button createRadioButton(
		Composite parent,
		int span,
		FormWidgetFactory factory,
		String[] choice) {
		Button button;
		if (factory == null) {
			button = new Button(parent, SWT.RADIO);
		} else {
			button = factory.createButton(parent, null, SWT.RADIO);
		}
		button.setData(choice[0]);
		button.setText(choice[1]);
		GridData gd = fill(button, span);
		gd.horizontalIndent = 10;
		return button;
	}

	public void setValue(Object value) {
		super.setValue(value);
		if (buttons != null && value != null) {
			selectChoice(value.toString());
		}
	}
	
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (labelControl!=null) {
			labelControl.setEnabled(enabled);
			for (int i=0; i<buttons.length; i++) {
				buttons[i].setEnabled(isEnabled());
			}
		}
	}

	private void selectChoice(String choice) {
		blockListener = true;
		for (int i = 0; i < buttons.length; i++) {
			Button button = buttons[i];
			String bname = button.getData().toString();
			if (bname.equals(choice)) {
				button.setSelection(true);
			} else {
				button.setSelection(false);
			}
		}
		blockListener = false;
	}
}