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
package org.eclipse.pde.internal.ui.util;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class ChoicePropertyDescriptor extends PropertyDescriptor {

	/**
	 * The list of possible values to display in the combo box
	 */
	private String[] values;

	/**
	 * Creates an property descriptor with the given id, display name, and list
	 * of value labels to display in the combo box cell editor.
	 * 
	 * @param id the id of the property
	 * @param displayName the name to display for the property
	 * @param valuesArray the list of possible values to display in the combo box
	 */
	public ChoicePropertyDescriptor(Object id, String displayName, String[] valuesArray) {
		super(id, displayName);
		values = valuesArray;
	}

	/**
	 * The <code>ComboBoxPropertyDescriptor</code> implementation of this 
	 * <code>IPropertyDescriptor</code> method creates and returns a new
	 * <code>ComboBoxCellEditor</code>.
	 * <p>
	 * The editor is configured with the current validator if there is one.
	 * </p>
	 */
	public CellEditor createPropertyEditor(Composite parent) {
		CellEditor editor = new ComboBoxCellEditor(parent, values, SWT.READ_ONLY);
		if (getValidator() != null)
			editor.setValidator(getValidator());
		return editor;
	}
}
