/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * This implementation of the TemplateOption can be used to represent options
 * that are boolean choices. Option provides the appropriate visual presentation
 * that allows users to set the boolean value of the option.
 * 
 * @since 2.0
 */
public class BooleanOption extends TemplateOption {
	private Button button;

	/**
	 * The constructor of the option.
	 * 
	 * @param section
	 *            the parent section
	 * @param name
	 *            the unique name
	 * @param label
	 *            the presentable label of the option
	 */
	public BooleanOption(BaseOptionTemplateSection section, String name, String label) {
		super(section, name, label);
	}

	/**
	 * Returns the current state of the option.
	 * 
	 * @return true of the option is selected, false otherwise.
	 */
	public boolean isSelected() {
		return getValue() != null && getValue().equals(Boolean.TRUE);
	}

	/**
	 * Changes the current state of the option to the provided state.
	 * 
	 * @param selected
	 *            the new state of the option
	 */
	public void setSelected(boolean selected) {
		setValue(selected ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Implementation of the superclass method that updates the option's widget
	 * with the new value.
	 * 
	 * @param value
	 *            the new option value
	 */
	public void setValue(Object value) {
		super.setValue(value);
		if (button != null)
			button.setSelection(isSelected());
	}

	/**
	 * Creates the boolean option control. Option reserves the right to modify
	 * the actual widget used as long as the user can modify its boolean state.
	 * 
	 * @param parent
	 *            the parent composite of the option widget
	 * @param span
	 *            the number of columns that the widget should span
	 */
	public void createControl(Composite parent, int span) {
		button = new Button(parent, SWT.CHECK);
		button.setText(getLabel());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		button.setLayoutData(gd);
		button.setSelection(isSelected());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BooleanOption.super.setValue(button.getSelection() ? Boolean.TRUE : Boolean.FALSE);
				getSection().validateOptions(BooleanOption.this);
			}
		});
		button.setEnabled(isEnabled());
	}

	/**
	 * Implementatin of the superclass method that updates the option widget
	 * with the new enabled state.
	 * 
	 * @param enabled
	 *            the new enabled state.
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (button != null)
			button.setEnabled(enabled);
	}
}
