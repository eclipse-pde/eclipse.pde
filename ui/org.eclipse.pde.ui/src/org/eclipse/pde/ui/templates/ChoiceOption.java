/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Implementation of the TemplateOption that allows users to choose a value from
 * the fixed set of options.
 *
 * @since 2.0
 * @deprecated see {@link RadioChoiceOption} and {@link ComboChoiceOption}
 */
@Deprecated
public class ChoiceOption extends TemplateOption {
	private final String[][] choices;
	private Control labelControl;
	private Button[] buttons;
	private boolean blockListener;

	/**
	 * Constructor for ChoiceOption.
	 *
	 * @param section
	 *            the parent section.
	 * @param name
	 *            the unique name
	 * @param label
	 *            the presentable label
	 * @param choices
	 *            the list of choices from which the value can be chosen. Each
	 *            array entry should be an array of size 2, where position 0
	 *            will be interpeted as the choice unique name, and position 1
	 *            as the choice presentable label.
	 */
	public ChoiceOption(BaseOptionTemplateSection section, String name, String label, String[][] choices) {
		super(section, name, label);
		this.choices = choices;
	}

	@Override
	public void createControl(Composite parent, int span) {
		Composite container = createComposite(parent, span);
		fill(container, span);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		labelControl = createLabel(container, span);
		labelControl.setEnabled(isEnabled());
		fill(labelControl, span);

		buttons = new Button[choices.length];

		SelectionListener listener = widgetSelectedAdapter(e -> {
			Button b = (Button) e.widget;
			if (blockListener)
				return;
			if (b.getSelection()) {
				ChoiceOption.super.setValue(b.getData().toString());
				getSection().validateOptions(ChoiceOption.this);
			}
		});

		for (int i = 0; i < choices.length; i++) {
			String[] choice = choices[i];
			Button button = createRadioButton(parent, span, choice);
			buttons[i] = button;
			button.addSelectionListener(listener);
			button.setEnabled(isEnabled());
		}
		if (getChoice() != null)
			selectChoice(getChoice());
	}

	/**
	 * Returns the string value of the current choice.
	 *
	 * @return the current choice or <samp>null </samp> if not initialized.
	 */
	public String getChoice() {
		return getValue() != null ? getValue().toString() : null;
	}

	/**
	 * Implements the superclass method by passing the new value to the option's
	 * widget.
	 *
	 * @param value
	 *            the new value.
	 */
	@Override
	public void setValue(Object value) {
		super.setValue(value);
		if (buttons != null && value != null) {
			selectChoice(value.toString());
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (labelControl != null) {
			labelControl.setEnabled(enabled);
			for (Button button : buttons) {
				button.setEnabled(isEnabled());
			}
		}
	}

	private GridData fill(Control control, int span) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
		return gd;
	}

	private Composite createComposite(Composite parent, int span) {
		Composite composite = new Composite(parent, SWT.NULL);
		fill(composite, span);
		return composite;
	}

	private Button createRadioButton(Composite parent, int span, String[] choice) {
		Button button = new Button(parent, SWT.RADIO);
		button.setData(choice[0]);
		button.setText(choice[1]);
		GridData gd = fill(button, span);
		gd.horizontalIndent = 10;
		return button;
	}

	private void selectChoice(String choice) {
		blockListener = true;
		for (Button button : buttons) {
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
