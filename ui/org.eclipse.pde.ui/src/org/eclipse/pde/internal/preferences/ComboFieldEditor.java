/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;

/**
 * @version 	1.0
 * @author
 */
public class ComboFieldEditor extends FieldEditor {
	private Combo combo;
	private String [] choices;
	
	
public ComboFieldEditor(String name, String labelText, String [] choices, Composite parent) {
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
	Combo control = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
	control.setItems(choices);
	return control;
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
		selectItem(value);
	}
	
	private void selectItem(String value) {
		if (combo != null) {
			for (int i=0; i<combo.getItemCount(); i++) {
				String item = combo.getItem(i);
				if (item.equals(value)) {
					combo.select(i);
					break;
				}
			}
		}
	}

	/*
	 * @see FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault() {
		String value = getPreferenceStore().getDefaultString(getPreferenceName());
		selectItem(value);
	}

	/*
	 * @see FieldEditor#doStore()
	 */
	protected void doStore() {
		int index = combo.getSelectionIndex();
		if (index!= -1) {
			String newValue = combo.getItem(index);
			getPreferenceStore().setValue(getPreferenceName(), newValue);
		}
	}

	/*
	 * @see FieldEditor#getNumberOfControls()
	 */
	public int getNumberOfControls() {
		return 2;
	}

}
