/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * This implementation of the TemplateOption can be
 * used to represent options that are boolean choices.
 * Option provides the appropriate visual presentation
 * that allows users to set the boolean value of the
 * option.
 * <p>
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public class BooleanOption extends TemplateOption {
	private Button button;
	/**
	 * The constructor of the option.
	 * @param section the parent section
	 * @param name the unique name 
	 * @param label the presentable label of the option
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public BooleanOption(
		BaseOptionTemplateSection section,
		String name,
		String label) {
		super(section, name, label);
	}
	/**
	 * Returns the current state of the option.
	 * @return true of the option is selected, false otherwise.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isSelected() {
		return getValue() != null && getValue().equals(Boolean.TRUE);
	}
	/**
	 * Changes the current state of the option to the provided state.
	 * @param selected the new state of the option
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setSelected(boolean selected) {
		setValue(selected ? Boolean.TRUE : Boolean.FALSE);
	}
	/**
	 * Implementation of the superclass method that updates the
	 * option's widget with the new value.
	 * @param value the new option value
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setValue(Object value) {
		super.setValue(value);
		if (button != null)
			button.setSelection(isSelected());
	}
	/**
	 * Creates the boolean option control. Option reserves the
	 * right to modify the actual widget used as long as the user
	 * can modify its boolean state.
	 * @param parent the parent composite of the option widget
	 * @param span the number of columns that the widget should span
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
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
				BooleanOption.super.setValue(
					button.getSelection() ? Boolean.TRUE : Boolean.FALSE);
				getSection().validateOptions(BooleanOption.this);
			}
		});
		button.setEnabled(isEnabled());
	}
	/**
	 * Implementatin of the superclass method that updates the
	 * option widget with the new enabled state.
	 * @param enabled the new enabled state.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (button != null)
			button.setEnabled(enabled);
	}
}
