/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.ui.util.Choice;

/**
 * @version 	1.0
 * @author
 */
public class ComboFieldEditor extends FieldEditor {
	private Combo combo;
	private Choice [] choices;
	
	
public ComboFieldEditor(String name, String labelText, Choice [] choices, Composite parent) {
	this.choices = choices;
	init(name, labelText);
	createControl(parent);
}

protected void adjustForNumColumns(int numColumns) {
	GridData gd = (GridData)combo.getLayoutData();
	gd.horizontalSpan = numColumns - 1;
	// We only grab excess space if we have to
	// If another field editor has more columns then
	// we assume it is setting the width.
	gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
}

public Combo getComboControl() {
	return combo;
}

public Combo getComboControl(Composite parent) {
	Combo control = new Combo(parent, SWT.BORDER);
	control.setItems(createItems());
	return control;
}

private String [] createItems() {
	if (choices==null) return new String[0];
	String [] items = new String[choices.length];
	for (int i=0; i<choices.length; i++) {
		items[i] = choices[i].getLabel();
	}
	return items;
}

protected void doFillIntoGrid(Composite parent, int numColumns) {
	getLabelControl(parent);

	combo = getComboControl(parent);
	GridData gd = new GridData();
	gd.horizontalSpan = numColumns - 1;
	gd.horizontalAlignment = gd.FILL;
	gd.grabExcessHorizontalSpace = true;
	combo.setLayoutData(gd);
}

	/*
	 * @see FieldEditor#doLoad()
	 */
	protected void doLoad() {
		String value = getPreferenceStore().getString(getPreferenceName());
		combo.setText(value);
	}
	
	/*
	 * @see FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault() {
		String value = getPreferenceStore().getDefaultString(getPreferenceName());
		combo.setText(value);
	}
	
	private int getIndexOf(String value) {
		for (int i=0; i<choices.length; i++) {
			Choice choice = choices[i];
			if (value.equals(choice.getValue())) {
				return i;
			}
		}
		return -1;
	}
	
	private void selectItem(String value) {
		int index = getIndexOf(value);
		if (index!= -1) combo.select(index);
		else
			combo.setText(value);
	}

	/*
	 * @see FieldEditor#doStore()
	 */
	protected void doStore() {
		String newValue = combo.getText();
		int index = getIndexOf(newValue);
		if (index != -1) {
			Choice choice = choices[index];
			newValue = choice.getValue();
		}
		getPreferenceStore().setValue(getPreferenceName(), newValue);
	}

	/*
	 * @see FieldEditor#getNumberOfControls()
	 */
	public int getNumberOfControls() {
		return 2;
	}
}
