package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;

public class CheckOption extends RuntimeOption {
	private Button button;

public CheckOption(String key, String label) {
	super(key, label);
}
public Control createControl(Composite parent) {
	button = new Button(parent, SWT.CHECK);
	button.setText(getLabel());
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	button.setLayoutData(gd);
	return button;
}
public String getValue() {
	return button.getSelection()?"true":"false";
}
public void setValue(Object value) {
	if (value != null) {
		String svalue = value.toString().toLowerCase();
		if (svalue.equals("true"))
			button.setSelection(true);
		else
			button.setSelection(false);
	}
}
}
