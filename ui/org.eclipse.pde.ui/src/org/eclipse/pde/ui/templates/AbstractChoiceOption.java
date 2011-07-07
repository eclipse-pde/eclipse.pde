/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 208534
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Abstract implementation of the TemplateOption that allows users to choose a value from
 * the fixed set of options.
 * 
 * @since 2.0
 */
public abstract class AbstractChoiceOption extends TemplateOption {
	protected String[][] fChoices;
	private boolean fBlockListener;

	/**
	 * Constructor for AbstractChoiceOption.
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
	public AbstractChoiceOption(BaseOptionTemplateSection section, String name, String label, String[][] choices) {
		super(section, name, label);
		this.fChoices = choices;
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
	public void setValue(Object value) {
		setValue(value, true);
	}

	/**
	 * Implements the superclass method by passing the new value to the option's
	 * widget; updates to the combo can be supressed.
	 * 
	 * @param value
	 *            the new value.
	 * @param updateControl
	 *            true to have the Combo's displayed value updated too, false to
	 *            indicate that this isn't necessary.
	 *            
	 * @since 3.4
	 */
	protected void setValue(Object value, boolean updateControl) {
		super.setValue(value);
		if (updateControl) {
			setOptionValue(value);
		}
	}

	protected abstract void setOptionValue(Object value);

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.TemplateOption#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setOptionEnabled(enabled);
	}

	protected abstract void setOptionEnabled(boolean enabled);

	protected GridData fill(Control control, int span) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
		return gd;
	}

	protected Composite createComposite(Composite parent, int span) {
		Composite composite = new Composite(parent, SWT.NULL);
		fill(composite, span);
		return composite;
	}

	protected void selectChoice(String choice) {
		fBlockListener = true;
		selectOptionChoice(choice);
		fBlockListener = false;
	}

	protected abstract void selectOptionChoice(String choice);

	protected boolean isBlocked() {
		return fBlockListener;
	}
}
